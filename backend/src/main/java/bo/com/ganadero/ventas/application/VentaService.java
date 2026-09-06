package bo.com.ganadero.ventas.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.movimientos.application.MovimientoCommand;
import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.MovimientoAnimal;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import bo.com.ganadero.pesajes.domain.EstadoPesaje;
import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.pesajes.domain.PesajeRepository;
import bo.com.ganadero.pesajes.domain.TipoPesaje;
import bo.com.ganadero.pesajes.domain.TipoPeso;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.ventas.domain.Venta;
import bo.com.ganadero.ventas.domain.VentaRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Registra el precio/comprador de la venta de un animal en un solo paso.
 *
 * <p>No reimplementa la máquina de estados de movimientos: compone con
 * {@link MovimientoService} (tipo SALIDA_VENTA) para que el animal pase a
 * VENDIDO, y guarda los datos comerciales en su propia tabla. El peso de
 * salida se apoya en el historial de {@link Pesaje}: reutiliza uno existente
 * (sin duplicarlo) o crea uno nuevo con motivo VENTA enlazado a esta venta.</p>
 */
@Service
public class VentaService {
    private final VentaRepository ventas;
    private final AnimalRepository animales;
    private final MovimientoService movimientos;
    private final PesajeRepository pesajes;
    private final UserContext context;
    private final ObjectProvider<RestriccionRetiroPort> restriccionRetiro;

    public VentaService(VentaRepository ventas, AnimalRepository animales, MovimientoService movimientos,
                        PesajeRepository pesajes, UserContext context, ObjectProvider<RestriccionRetiroPort> restriccionRetiro) {
        this.ventas = ventas;
        this.animales = animales;
        this.movimientos = movimientos;
        this.pesajes = pesajes;
        this.context = context;
        this.restriccionRetiro = restriccionRetiro;
    }

    @Transactional
    public Venta registrar(VentaCommand command) {
        CurrentUser user = context.requirePermission("VENTA_REGISTRAR");
        if (command.animalId() == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (command.precio() == null || command.precio().signum() <= 0) {
            throw new BusinessException(ErrorCode.VENTA_PRECIO_INVALIDO);
        }
        if (command.comprador() == null || command.comprador().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        Animal animal = animales.findById(command.animalId(), user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        if (animal.estado() == EstadoAnimal.VENDIDO) {
            throw new BusinessException(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED, "El animal ya fue vendido.");
        }
        LocalDate fecha = command.fechaVenta() == null ? LocalDate.now() : command.fechaVenta();

        RestriccionRetiroPort retiro = restriccionRetiro.getIfAvailable();
        if (retiro != null) {
            retiro.vigente(user.empresaId(), animal.id(), fecha).ifPresent(r -> {
                throw new BusinessException(ErrorCode.VENTA_RETIRO_SANITARIO_VIGENTE,
                        "El animal tiene un retiro de " + r.tipo() + " vigente hasta " + r.hasta() + ".");
            });
        }

        Pesaje pesajeReferenciado = null;
        if (command.pesajeExistenteId() != null) {
            Pesaje existente = pesajes.findById(command.pesajeExistenteId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VENTA_PESAJE_INVALIDO));
            if (!existente.animalId().equals(animal.id()) || existente.estado() != EstadoPesaje.ACTIVO) {
                throw new BusinessException(ErrorCode.VENTA_PESAJE_INVALIDO);
            }
            pesajeReferenciado = existente;
        }

        Movimiento movimiento = movimientos.create(new MovimientoCommand(null, TipoMovimiento.SALIDA_VENTA, fecha,
                "Venta a " + command.comprador(), command.observaciones(),
                null, null, null, null, null, null,
                List.of(new MovimientoAnimal(animal.id(), animal.version()))));
        movimientos.confirm(movimiento.id(), movimiento.version());

        BigDecimal pesoKg;
        if (pesajeReferenciado != null) {
            pesoKg = pesajeReferenciado.pesoKg();
        } else if (command.pesoVentaKg() != null) {
            if (command.pesoVentaKg().signum() <= 0) throw new BusinessException(ErrorCode.PESAJE_PESO_INVALIDO);
            pesoKg = command.pesoVentaKg();
        } else {
            pesoKg = null;
        }

        // La Venta se persiste antes que cualquier Pesaje nuevo: pesaje.venta_id tiene FK a venta(id).
        UUID ventaId = UUID.randomUUID();
        Venta venta = new Venta(ventaId, animal.id(), movimiento.id(), fecha, command.comprador(),
                command.precio(), command.moneda() == null || command.moneda().isBlank() ? "BOB" : command.moneda(),
                pesoKg, command.observaciones(), user.userId(), Instant.now(), 0);
        Venta guardada = ventas.create(venta);

        if (pesajeReferenciado == null && command.pesoVentaKg() != null) {
            crearPesajeVenta(user, command, animal, fecha, movimiento.id(), ventaId);
        }
        return guardada;
    }

    /** Crea un pesaje nuevo (motivo VENTA) enlazado a una venta ya persistida. */
    private void crearPesajeVenta(CurrentUser user, VentaCommand command, Animal animal, LocalDate fecha,
                                  UUID movimientoId, UUID ventaId) {
        UUID id = UUID.randomUUID();
        Pesaje nuevo = new Pesaje(id, user.empresaId(), animal.id(), fecha, command.pesoVentaKg(),
                TipoPesaje.VENTA, command.tipoPeso() == null ? TipoPeso.MEDIDO : command.tipoPeso(),
                null, null, user.userId(), animal.propiedadActualId(), animal.potreroActualId(),
                animal.loteActualId(), command.dispositivo(), null, ventaId, movimientoId, id, null,
                EstadoPesaje.ACTIVO, null, null, null, "Peso de salida registrado al vender.",
                null, null, null, null, null, null, 0);
        pesajes.create(nuevo, user.userId());
    }

    @Transactional(readOnly = true)
    public Venta get(UUID id) {
        context.requirePermission("VENTA_VER");
        return ventas.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.VENTA_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Venta> list(UUID animalId, LocalDate desde, LocalDate hasta) {
        context.requirePermission("VENTA_VER");
        return ventas.findAll(animalId, desde, hasta);
    }
}
