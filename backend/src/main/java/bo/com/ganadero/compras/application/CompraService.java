package bo.com.ganadero.compras.application;

import bo.com.ganadero.animales.application.AnimalCommand;
import bo.com.ganadero.animales.application.AnimalService;
import bo.com.ganadero.animales.application.EstimacionEdadAnimal;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.compras.domain.*;
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
import bo.com.ganadero.proveedores.application.ProveedorCommand;
import bo.com.ganadero.proveedores.application.ProveedorService;
import bo.com.ganadero.proveedores.domain.Proveedor;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Toda incorporación de animales comprados (individual o por lote) pertenece a una Compra
 * formal. Reutiliza {@link AnimalService#create} por cada detalle, {@link MovimientoService}
 * (tipo INGRESO_COMPRA, igual que ya hacía IngresoLotePage.tsx desde el cliente) y el
 * repositorio de {@link Pesaje} para el peso de ingreso — toda la orquestación queda en una
 * sola transacción de backend en vez de varias llamadas encadenadas desde el navegador.
 */
@Service
public class CompraService {
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");
    private static final int ESCALA_MONETARIA = 2;

    private final CompraRepository compras;
    private final ProveedorService proveedores;
    private final AnimalService animales;
    private final MovimientoService movimientos;
    private final PesajeRepository pesajes;
    private final UserContext context;
    private final CodigoService codigos;
    private final ApplicationEventPublisher events;

    public CompraService(CompraRepository compras, ProveedorService proveedores, AnimalService animales,
                         MovimientoService movimientos, PesajeRepository pesajes, UserContext context,
                         CodigoService codigos, ApplicationEventPublisher events) {
        this.compras = compras;
        this.proveedores = proveedores;
        this.animales = animales;
        this.movimientos = movimientos;
        this.pesajes = pesajes;
        this.context = context;
        this.codigos = codigos;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public CompraPage list(EstadoCompra estado, int page, int size) {
        context.requirePermission("COMPRA_VER");
        return compras.list(estado, page, size);
    }

    @Transactional(readOnly = true)
    public Compra get(UUID id) {
        context.requirePermission("COMPRA_VER");
        return require(id);
    }

    @Transactional(readOnly = true)
    public List<CompraDetalle> detalles(UUID id) {
        context.requirePermission("COMPRA_VER");
        require(id);
        return compras.findDetalles(id);
    }

    /** Resumen de compra para la ficha del animal (tarjeta "Compra"): un solo viaje, sin exponer todo el encabezado. */
    public record ResumenCompraAnimal(UUID id, String codigo, Instant fechaRecepcion, String modalidad, String moneda,
                                      BigDecimal precioAsignado, String proveedorNombre, String proveedorTelefono,
                                      String proveedorDocumento) {}

    @Transactional(readOnly = true)
    public Optional<ResumenCompraAnimal> resumenParaAnimal(UUID animalId) {
        context.requirePermission("COMPRA_VER");
        return compras.findByAnimalId(animalId).map(c -> {
            CompraDetalle detalle = compras.findDetalles(c.id()).stream()
                    .filter(d -> animalId.equals(d.animalId())).findFirst().orElse(null);
            Proveedor proveedor = proveedores.get(c.proveedorId());
            return new ResumenCompraAnimal(c.id(), c.codigo(), c.fechaRecepcion(), c.modalidad().name(), c.moneda(),
                    detalle == null ? null : detalle.precioAsignado(), proveedor.nombre(), proveedor.telefono(),
                    proveedor.documento());
        });
    }

    @Transactional
    public Compra crearBorrador(CompraCommand c) {
        CurrentUser u = context.requirePermission("COMPRA_CREAR");
        Proveedor proveedor = resolverProveedor(c, u);
        validarEncabezado(c);
        if (c.detalles() == null || c.detalles().isEmpty()) throw new BusinessException(ErrorCode.COMPRA_SIN_ANIMALES);

        List<BigDecimal> preciosPorLinea = calcularPrecios(c);
        BigDecimal total = preciosPorLinea.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal referencial = total.divide(BigDecimal.valueOf(c.detalles().size()), ESCALA_MONETARIA, RoundingMode.HALF_UP);

        String codigo = codigos.paraCreacion(u, TipoCodigo.COMPRA, null, null, null);
        UUID id = UUID.randomUUID();
        Compra compra = new Compra(id, codigo, proveedor.id(), c.fechaRecepcion(), c.modalidad(), c.moneda(),
                c.detalles().size(), c.precioUnitario(), total, referencial, c.propiedadId(), c.potreroId(),
                c.loteGanaderoId(), c.proposito(), c.observaciones(), EstadoCompra.BORRADOR, null, null, null,
                null, Instant.now(), u.userId(), Instant.now(), u.userId(), 0);

        List<CompraDetalle> detalles = construirDetalles(id, c, preciosPorLinea);
        Compra saved = compras.crear(compra, detalles, u.userId());
        audit(u, "CREAR_BORRADOR", saved.id());
        return saved;
    }

    @Transactional
    public Compra actualizarBorrador(UUID id, CompraCommand c) {
        CurrentUser u = context.requirePermission("COMPRA_EDITAR");
        Compra actual = require(id);
        if (actual.estado() != EstadoCompra.BORRADOR) throw new BusinessException(ErrorCode.COMPRA_ESTADO_INVALIDO,
                "Solo puede editarse una compra en borrador.");
        Proveedor proveedor = resolverProveedor(c, u);
        validarEncabezado(c);
        if (c.detalles() == null || c.detalles().isEmpty()) throw new BusinessException(ErrorCode.COMPRA_SIN_ANIMALES);

        List<BigDecimal> preciosPorLinea = calcularPrecios(c);
        BigDecimal total = preciosPorLinea.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal referencial = total.divide(BigDecimal.valueOf(c.detalles().size()), ESCALA_MONETARIA, RoundingMode.HALF_UP);

        Compra editada = new Compra(id, actual.codigo(), proveedor.id(), c.fechaRecepcion(), c.modalidad(),
                c.moneda(), c.detalles().size(), c.precioUnitario(), total, referencial, c.propiedadId(),
                c.potreroId(), c.loteGanaderoId(), c.proposito(), c.observaciones(), EstadoCompra.BORRADOR,
                null, null, null, actual.origenMigracion(), actual.createdAt(), actual.createdBy(), null, null,
                Objects.requireNonNull(actual.version()));

        List<CompraDetalle> detalles = construirDetalles(id, c, preciosPorLinea);
        Compra saved = compras.actualizarBorrador(editada, detalles, u.userId());
        audit(u, "ACTUALIZAR_BORRADOR", saved.id());
        return saved;
    }

    @Transactional
    public Compra confirmar(UUID id, long version) {
        CurrentUser u = context.requirePermission("COMPRA_CONFIRMAR");
        Compra actual = require(id);
        if (actual.estado() == EstadoCompra.CONFIRMADA) return actual;
        if (actual.estado() == EstadoCompra.ANULADA) throw new BusinessException(ErrorCode.COMPRA_ESTADO_INVALIDO);
        List<CompraDetalle> detalles = compras.findDetalles(id);
        if (detalles.isEmpty()) throw new BusinessException(ErrorCode.COMPRA_SIN_ANIMALES);

        LocalDate fechaRecepcionDia = actual.fechaRecepcion().atZone(BOLIVIA).toLocalDate();

        // Todos los animales de una compra llegan a la misma propiedad/potrero/lote de recepción
        // (un único movimiento INGRESO_COMPRA para todo el lote, igual que hacía IngresoLotePage);
        // la propiedad/potrero por detalle queda como registro histórico de lo declarado en la línea.
        List<Animal> animalesCreados = new ArrayList<>();
        for (CompraDetalle d : detalles) {
            AnimalCommand cmd = new AnimalCommand(null, d.codigoSolicitado(), d.nombre(), d.sexo(),
                    d.fechaNacimiento(), d.fechaNacimientoEstimada(), d.razaId(), d.categoriaActualId(), null,
                    d.proposito(), bo.com.ganadero.animales.domain.OrigenAnimal.COMPRADO, actual.propiedadId(),
                    actual.potreroId(), null, fechaRecepcionDia, d.precioAsignado(),
                    null, null, null, d.observaciones(), 0L, d.pesoIngresoKg(),
                    d.pesoIngresoKg() == null ? null : d.tipoPeso() == TipoPeso.ESTIMADO, false, false,
                    d.edadDeclaradaValor(), d.edadDeclaradaUnidad(), d.fechaReferenciaEdad(),
                    d.fuenteEdadDeclarada(), d.observacionEstimacion(), d.categoriaManualMotivo(), false, null);
            Animal creado = animales.create(cmd);
            compras.asignarAnimal(d.id(), creado.id());
            animalesCreados.add(creado);
        }

        Movimiento movimiento = movimientos.create(new MovimientoCommand(null, TipoMovimiento.INGRESO_COMPRA,
                fechaRecepcionDia, "Compra " + actual.codigo(), actual.observaciones(), null, null, null,
                actual.propiedadId(), actual.potreroId(), actual.loteGanaderoId(),
                animalesCreados.stream().map(a -> new MovimientoAnimal(a.id(), a.version())).toList()));
        movimientos.confirm(movimiento.id(), movimiento.version());

        for (int i = 0; i < detalles.size(); i++) {
            CompraDetalle d = detalles.get(i);
            if (d.pesoIngresoKg() == null) continue;
            Animal animal = animalesCreados.get(i);
            UUID pesajeId = UUID.randomUUID();
            Pesaje pesaje = new Pesaje(pesajeId, u.empresaId(), animal.id(), fechaRecepcionDia, d.pesoIngresoKg(),
                    TipoPesaje.COMPRA, d.tipoPeso(), null, null, u.userId(), d.propiedadId(), d.potreroId(), null,
                    d.metodoPeso(), actual.id(), null, movimiento.id(), pesajeId, null, EstadoPesaje.ACTIVO, null, null, null,
                    "Peso registrado al confirmar la compra " + actual.codigo() + ".", null, null, null, null, null,
                    null, 0);
            pesajes.create(pesaje, u.userId());
        }

        Compra confirmada = compras.confirmar(id, version, u.userId());
        audit(u, "CONFIRMAR", confirmada.id());
        return confirmada;
    }

    @Transactional(readOnly = true)
    public List<DependenciaCompra> verificarDependencias(UUID id) {
        context.requirePermission("COMPRA_VER");
        return dependenciasDeTodosLosDetalles(id);
    }

    @Transactional
    public Compra anular(UUID id, String motivo, long version) {
        CurrentUser u = context.requirePermission("COMPRA_ANULAR");
        if (motivo == null || motivo.isBlank()) throw new BusinessException(ErrorCode.COMPRA_MOTIVO_REQUERIDO);
        Compra actual = require(id);
        if (actual.estado() != EstadoCompra.CONFIRMADA) throw new BusinessException(ErrorCode.COMPRA_ESTADO_INVALIDO,
                "Solo puede anularse una compra confirmada.");

        List<DependenciaCompra> dependencias = dependenciasDeTodosLosDetalles(id);
        if (!dependencias.isEmpty()) {
            String detalle = dependencias.stream().map(DependenciaCompra::tipo).distinct()
                    .reduce((a, b) -> a + ", " + b).orElse("");
            throw new BusinessException(ErrorCode.COMPRA_TIENE_DEPENDENCIAS,
                    "La compra tiene animales con eventos posteriores (" + detalle + "); no puede anularse directamente.");
        }

        for (CompraDetalle d : compras.findDetalles(id)) {
            if (d.animalId() == null) continue;
            Animal animal = animales.get(d.animalId());
            if (animal.estado() == EstadoAnimal.ACTIVO) {
                animales.changeState(animal.id(), EstadoAnimal.DESCARTADO, "Compra anulada: " + motivo, animal.version());
            }
        }

        Compra anulada = compras.anular(id, motivo, version, u.userId());
        audit(u, "ANULAR", anulada.id());
        return anulada;
    }

    private List<CompraDetalle> construirDetalles(UUID compraId, CompraCommand c, List<BigDecimal> preciosPorLinea) {
        List<CompraDetalle> detalles = new ArrayList<>();
        for (int i = 0; i < c.detalles().size(); i++) {
            CompraDetalleCommand item = c.detalles().get(i);
            if (item.pesoIngresoKg() != null && item.tipoPeso() == null) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "Indica si el peso del animal " + (i + 1) + " es medido o estimado.");
            }
            EstimacionEdadAnimal.Resultado edad = EstimacionEdadAnimal.resolver(item.fechaNacimiento(),
                    item.fechaNacimientoEstimada(), item.edadDeclaradaValor(), item.edadDeclaradaUnidad(),
                    item.fechaReferenciaEdad(), item.fuenteEdadDeclarada(), item.observacionEstimacion());
            detalles.add(new CompraDetalle(UUID.randomUUID(), compraId, null, i + 1, preciosPorLinea.get(i),
                    item.pesoIngresoKg(), item.tipoPeso(), item.metodoPeso(),
                    item.propiedadId() != null ? item.propiedadId() : c.propiedadId(),
                    item.potreroId() != null ? item.potreroId() : c.potreroId(),
                    item.loteGanaderoId() != null ? item.loteGanaderoId() : c.loteGanaderoId(),
                    item.codigoSolicitado(), item.nombre(), item.sexo(), item.razaId(), item.proposito(),
                    edad.fechaNacimiento(), edad.estimada(), edad.valorDeclarado(), edad.unidad(),
                    edad.fechaReferencia(), edad.fuente(), edad.detalle(), item.categoriaActualId(),
                    item.categoriaManualMotivo(), item.observaciones(), Instant.now()));
        }
        return detalles;
    }

    private List<DependenciaCompra> dependenciasDeTodosLosDetalles(UUID compraId) {
        List<DependenciaCompra> resultado = new ArrayList<>();
        for (CompraDetalle d : compras.findDetalles(compraId)) {
            if (d.animalId() == null) continue;
            resultado.addAll(compras.dependenciasPosteriores(d.animalId(), compraId));
        }
        return resultado;
    }

    private Proveedor resolverProveedor(CompraCommand c, CurrentUser u) {
        ProveedorCommand pc = c.proveedorId() != null
                ? new ProveedorCommand(c.proveedorId(), null, null, null, null, null, null, null)
                : c.proveedorNuevo();
        if (pc == null) throw new BusinessException(ErrorCode.PROVEEDOR_DATOS_REQUERIDOS);
        return proveedores.buscarOCrear(pc, u);
    }

    private void validarEncabezado(CompraCommand c) {
        if (c.moneda() == null || c.moneda().isBlank()) throw new BusinessException(ErrorCode.COMPRA_MONEDA_REQUERIDA);
        if (c.modalidad() == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La modalidad de precio es obligatoria.");
        if (c.propiedadId() == null || c.potreroId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La propiedad y el potrero de recepción son obligatorios.");
        }
        if (c.modalidad() == ModalidadPrecio.POR_UNIDAD
                && (c.precioUnitario() == null || c.precioUnitario().signum() < 0)) {
            throw new BusinessException(ErrorCode.COMPRA_PRECIO_INVALIDO, "Indica un precio unitario válido (mayor o igual a cero).");
        }
        if (c.modalidad() == ModalidadPrecio.POR_TROPA
                && (c.precioTotal() == null || c.precioTotal().signum() < 0)) {
            throw new BusinessException(ErrorCode.COMPRA_PRECIO_INVALIDO, "Indica un precio total válido (mayor o igual a cero).");
        }
    }

    /** Nunca confía en el total enviado por el frontend: siempre recalcula por línea. */
    private List<BigDecimal> calcularPrecios(CompraCommand c) {
        for (CompraDetalleCommand item : c.detalles()) {
            if (item.precioOverride() != null && item.precioOverride().signum() < 0) {
                throw new BusinessException(ErrorCode.COMPRA_PRECIO_INVALIDO);
            }
        }
        int n = c.detalles().size();
        if (c.modalidad() == ModalidadPrecio.POR_UNIDAD) {
            return c.detalles().stream()
                    .map(item -> item.precioOverride() != null ? item.precioOverride() : c.precioUnitario())
                    .toList();
        }
        boolean algunOverride = c.detalles().stream().anyMatch(item -> item.precioOverride() != null);
        if (!algunOverride) {
            BigDecimal unidad = c.precioTotal().divide(BigDecimal.valueOf(n), ESCALA_MONETARIA, RoundingMode.HALF_UP);
            List<BigDecimal> resultado = new ArrayList<>();
            BigDecimal acumulado = BigDecimal.ZERO;
            for (int i = 0; i < n - 1; i++) {
                resultado.add(unidad);
                acumulado = acumulado.add(unidad);
            }
            resultado.add(c.precioTotal().subtract(acumulado));
            return resultado;
        }
        boolean todosConOverride = c.detalles().stream().allMatch(item -> item.precioOverride() != null);
        if (!todosConOverride) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "En una compra por tropa con ajustes manuales, todos los animales deben tener un precio asignado explícito.");
        }
        BigDecimal suma = c.detalles().stream().map(CompraDetalleCommand::precioOverride)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP)
                .compareTo(c.precioTotal().setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP)) != 0) {
            throw new BusinessException(ErrorCode.COMPRA_TOTAL_INCONSISTENTE,
                    "La suma de los precios asignados (" + suma + ") no coincide con el precio total de la compra (" + c.precioTotal() + ").");
        }
        return c.detalles().stream().map(CompraDetalleCommand::precioOverride).toList();
    }

    private Compra require(UUID id) {
        return compras.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.COMPRA_NOT_FOUND));
    }

    private void audit(CurrentUser u, String accion, UUID id) {
        events.publishEvent(new CompraAuditEvent(u.empresaId(), u.userId(), accion, id, Instant.now()));
    }
}
