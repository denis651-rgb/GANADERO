package bo.com.ganadero.ventas.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.movimientos.application.MovimientoCommand;
import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.MovimientoAnimal;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.ventas.domain.Venta;
import bo.com.ganadero.ventas.domain.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Registra el precio/comprador de la venta de un animal en un solo paso.
 *
 * <p>No reimplementa la máquina de estados de movimientos: compone con
 * {@link MovimientoService} (tipo SALIDA_VENTA) para que el animal pase a
 * VENDIDO, y guarda los datos comerciales en su propia tabla.</p>
 */
@Service
public class VentaService {
    private final VentaRepository ventas;
    private final AnimalRepository animales;
    private final MovimientoService movimientos;
    private final UserContext context;

    public VentaService(VentaRepository ventas, AnimalRepository animales, MovimientoService movimientos,
                        UserContext context) {
        this.ventas = ventas;
        this.animales = animales;
        this.movimientos = movimientos;
        this.context = context;
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

        Movimiento movimiento = movimientos.create(new MovimientoCommand(null, TipoMovimiento.SALIDA_VENTA, fecha,
                "Venta a " + command.comprador(), command.observaciones(),
                null, null, null, null, null, null,
                List.of(new MovimientoAnimal(animal.id(), animal.version()))));
        movimientos.confirm(movimiento.id(), movimiento.version());

        Venta venta = new Venta(UUID.randomUUID(), animal.id(), movimiento.id(), fecha, command.comprador(),
                command.precio(), command.moneda() == null || command.moneda().isBlank() ? "BOB" : command.moneda(),
                command.pesoVentaKg(), command.observaciones(), user.userId(), Instant.now(), 0);
        return ventas.create(venta);
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
