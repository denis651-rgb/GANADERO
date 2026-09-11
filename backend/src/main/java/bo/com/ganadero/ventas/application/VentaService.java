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
import bo.com.ganadero.ventas.domain.ModalidadVenta;
import bo.com.ganadero.ventas.domain.Venta;
import bo.com.ganadero.ventas.domain.VentaRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registra el precio/comprador de la venta de uno o varios animales.
 *
 * <p>No reimplementa la máquina de estados de movimientos: compone con
 * {@link MovimientoService} (tipo SALIDA_VENTA) para que el animal pase a
 * VENDIDO, y guarda los datos comerciales en su propia tabla. El peso de
 * salida se apoya en el historial de {@link Pesaje}: reutiliza uno existente
 * (sin duplicarlo, solo disponible para la venta individual) o crea uno nuevo
 * con motivo VENTA enlazado a esta venta.</p>
 *
 * <p>{@link #registrarLote(VentaLoteCommand)} vende varios animales en una sola
 * transacción (todo o nada), bajo dos modalidades: {@link ModalidadVenta#EN_PIE}
 * (un precio fijo por cabeza, igual para todos) o {@link ModalidadVenta#CARNEADO}
 * (precio por kilo, monto distinto por animal según {@code pesosVentaKg}).</p>
 */
@Service
public class VentaService {
    private static final int ESCALA_MONETARIA = 2;

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

        Pesaje pesajeReferenciado = null;
        if (command.pesajeExistenteId() != null) {
            Pesaje existente = pesajes.findById(command.pesajeExistenteId(), user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VENTA_PESAJE_INVALIDO));
            if (!existente.animalId().equals(animal.id()) || existente.estado() != EstadoPesaje.ACTIVO) {
                throw new BusinessException(ErrorCode.VENTA_PESAJE_INVALIDO);
            }
            pesajeReferenciado = existente;
        }

        String moneda = command.moneda() == null || command.moneda().isBlank() ? "BOB" : command.moneda();
        return procesarUnaVenta(user, animal, fecha, command.comprador(), command.telefonoComprador(),
                command.modalidad() == null ? ModalidadVenta.EN_PIE : command.modalidad(), command.precio(), null,
                moneda, pesajeReferenciado, command.pesoVentaKg(), command.tipoPeso(), command.dispositivo(),
                command.observaciones(), null);
    }

    @Transactional
    public List<Venta> registrarLote(VentaLoteCommand command) {
        CurrentUser user = context.requirePermission("VENTA_REGISTRAR");
        if (command.animalIds() == null || command.animalIds().isEmpty()) {
            throw new BusinessException(ErrorCode.VENTA_LOTE_VACIO);
        }
        if (command.comprador() == null || command.comprador().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (command.modalidad() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La modalidad de venta es obligatoria.");
        }
        if (command.modalidad() == ModalidadVenta.EN_PIE
                && (command.precioCabeza() == null || command.precioCabeza().signum() <= 0)) {
            throw new BusinessException(ErrorCode.VENTA_PRECIO_INVALIDO, "Indica un precio por cabeza válido.");
        }
        if (command.modalidad() == ModalidadVenta.CARNEADO
                && (command.precioKg() == null || command.precioKg().signum() <= 0)) {
            throw new BusinessException(ErrorCode.VENTA_PRECIO_INVALIDO, "Indica un precio por kilo válido.");
        }
        LocalDate fecha = command.fechaVenta() == null ? LocalDate.now() : command.fechaVenta();
        Map<UUID, BigDecimal> pesos = command.pesosVentaKg() == null ? Map.of() : command.pesosVentaKg();
        UUID grupoVentaId = UUID.randomUUID();

        List<Venta> resultado = new java.util.ArrayList<>();
        for (UUID animalId : new LinkedHashSet<>(command.animalIds())) {
            Animal animal = animales.findById(animalId, user.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
            if (animal.estado() == EstadoAnimal.VENDIDO) {
                throw new BusinessException(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED,
                        "El animal " + animal.codigo() + " ya fue vendido.");
            }
            BigDecimal peso = pesos.get(animalId);
            if (command.modalidad() == ModalidadVenta.CARNEADO && (peso == null || peso.signum() <= 0)) {
                throw new BusinessException(ErrorCode.VENTA_PESO_REQUERIDO,
                        "Falta el peso de salida del animal " + animal.codigo() + " para calcular su precio.");
            }
            if (peso != null && peso.signum() <= 0) throw new BusinessException(ErrorCode.PESAJE_PESO_INVALIDO);

            BigDecimal precioUnitario = command.modalidad() == ModalidadVenta.EN_PIE
                    ? command.precioCabeza() : command.precioKg();
            BigDecimal precio = command.modalidad() == ModalidadVenta.EN_PIE
                    ? command.precioCabeza()
                    : command.precioKg().multiply(peso).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);

            resultado.add(procesarUnaVenta(user, animal, fecha, command.comprador(), command.telefonoComprador(),
                    command.modalidad(), precio, precioUnitario, "BOB", null, peso, TipoPeso.MEDIDO, "WEB",
                    command.observaciones(), grupoVentaId));
        }
        return resultado;
    }

    /**
     * Núcleo compartido de una venta individual: valida retiro sanitario, confirma el movimiento
     * SALIDA_VENTA (única fuente de verdad para el cambio de estado del animal), persiste la
     * {@link Venta} y, si corresponde, el {@link Pesaje} de salida.
     */
    private Venta procesarUnaVenta(CurrentUser user, Animal animal, LocalDate fecha, String comprador,
                                   String telefonoComprador, ModalidadVenta modalidad, BigDecimal precio,
                                   BigDecimal precioUnitario, String moneda, Pesaje pesajeReferenciado, BigDecimal pesoNuevo,
                                   TipoPeso tipoPeso, String dispositivo, String observaciones, UUID grupoVentaId) {
        RestriccionRetiroPort retiro = restriccionRetiro.getIfAvailable();
        if (retiro != null) {
            retiro.vigente(user.empresaId(), animal.id(), fecha).ifPresent(r -> {
                throw new BusinessException(ErrorCode.VENTA_RETIRO_SANITARIO_VIGENTE,
                        "El animal tiene un retiro de " + r.tipo() + " vigente hasta " + r.hasta() + ".");
            });
        }

        Movimiento movimiento = movimientos.create(new MovimientoCommand(null, TipoMovimiento.SALIDA_VENTA, fecha,
                "Venta a " + comprador, observaciones,
                null, null, null, null, null, null,
                List.of(new MovimientoAnimal(animal.id(), animal.version()))));
        movimientos.confirm(movimiento.id(), movimiento.version());

        BigDecimal pesoKg;
        if (pesajeReferenciado != null) {
            pesoKg = pesajeReferenciado.pesoKg();
        } else if (pesoNuevo != null) {
            if (pesoNuevo.signum() <= 0) throw new BusinessException(ErrorCode.PESAJE_PESO_INVALIDO);
            pesoKg = pesoNuevo;
        } else {
            pesoKg = null;
        }

        // La Venta se persiste antes que cualquier Pesaje nuevo: pesaje.venta_id tiene FK a venta(id).
        UUID ventaId = UUID.randomUUID();
        Venta venta = new Venta(ventaId, animal.id(), movimiento.id(), fecha, comprador, precio,
                moneda, pesoKg, observaciones, user.userId(), Instant.now(), 0, telefonoComprador, modalidad,
                precioUnitario, grupoVentaId);
        Venta guardada = ventas.create(venta);

        if (pesajeReferenciado == null && pesoNuevo != null) {
            crearPesajeVenta(user, animal, fecha, movimiento.id(), ventaId, pesoNuevo, tipoPeso, dispositivo);
        }
        return guardada;
    }

    /** Crea un pesaje nuevo (motivo VENTA) enlazado a una venta ya persistida. */
    private void crearPesajeVenta(CurrentUser user, Animal animal, LocalDate fecha, UUID movimientoId, UUID ventaId,
                                  BigDecimal pesoVentaKg, TipoPeso tipoPeso, String dispositivo) {
        UUID id = UUID.randomUUID();
        Pesaje nuevo = new Pesaje(id, user.empresaId(), animal.id(), fecha, pesoVentaKg,
                TipoPesaje.VENTA, tipoPeso == null ? TipoPeso.MEDIDO : tipoPeso,
                null, null, user.userId(), animal.propiedadActualId(), animal.potreroActualId(),
                animal.loteActualId(), dispositivo, null, ventaId, movimientoId, id, null,
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
