package bo.com.ganadero.proveedores.application;

import bo.com.ganadero.proveedores.domain.Proveedor;
import bo.com.ganadero.proveedores.domain.ProveedorRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Proveedor formal: antes el dato vivía como texto libre en las observaciones del animal
 * (ver IngresoLotePage.tsx, `Proveedor: <nombre>`). Ahora las compras se relacionan con esta
 * entidad — se puede buscar uno existente o crear uno nuevo sin salir del formulario de compra.
 */
@Service
public class ProveedorService {
    private final ProveedorRepository proveedores;
    private final UserContext context;
    private final ApplicationEventPublisher events;

    public ProveedorService(ProveedorRepository proveedores, UserContext context, ApplicationEventPublisher events) {
        this.proveedores = proveedores;
        this.context = context;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> buscar(String query, boolean soloActivos) {
        context.requirePermission("PROVEEDOR_VER");
        return proveedores.buscar(query, soloActivos);
    }

    @Transactional(readOnly = true)
    public Proveedor get(UUID id) {
        context.requirePermission("PROVEEDOR_VER");
        return require(id);
    }

    @Transactional
    public Proveedor crear(ProveedorCommand command) {
        CurrentUser u = context.requirePermission("PROVEEDOR_CREAR");
        return crearInterno(command, u);
    }

    @Transactional
    public Proveedor actualizar(UUID id, ProveedorCommand command) {
        CurrentUser u = context.requirePermission("PROVEEDOR_EDITAR");
        Proveedor actual = require(id);
        validarDatos(command);
        Proveedor editado = new Proveedor(id, command.nombre().trim(), limpiar(command.telefono()),
                limpiar(command.documento()), limpiar(command.direccion()), limpiar(command.correo()),
                limpiar(command.observaciones()), actual.activo(), actual.createdAt(), actual.createdBy(),
                null, null, Objects.requireNonNull(command.version()));
        Proveedor saved = proveedores.actualizar(editado, u.userId());
        audit(u, "ACTUALIZAR", saved.id());
        return saved;
    }

    @Transactional
    public Proveedor cambiarEstado(UUID id, boolean activo, long version) {
        CurrentUser u = context.requirePermission("PROVEEDOR_EDITAR");
        require(id);
        Proveedor saved = proveedores.cambiarEstado(id, activo, version, u.userId());
        audit(u, activo ? "ACTIVAR" : "DESACTIVAR", saved.id());
        return saved;
    }

    /**
     * Usado por CompraService: reutiliza un proveedor existente por id o por documento; si no
     * hay coincidencia, crea uno nuevo. Nunca duplica por documento/NIT (índice único).
     */
    @Transactional
    public Proveedor buscarOCrear(ProveedorCommand command, CurrentUser actor) {
        if (command.id() != null) {
            Proveedor existente = require(command.id());
            if (!existente.activo()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR, "El proveedor seleccionado está inactivo.");
            }
            return existente;
        }
        validarDatos(command);
        if (command.documento() != null && !command.documento().isBlank()) {
            var porDocumento = proveedores.findByDocumento(command.documento().trim());
            if (porDocumento.isPresent()) return porDocumento.get();
        }
        return crearInterno(command, actor);
    }

    private Proveedor crearInterno(ProveedorCommand command, CurrentUser actor) {
        validarDatos(command);
        UUID id = UUID.randomUUID();
        Proveedor nuevo = new Proveedor(id, command.nombre().trim(), limpiar(command.telefono()),
                limpiar(command.documento()), limpiar(command.direccion()), limpiar(command.correo()),
                limpiar(command.observaciones()), true, Instant.now(), actor.userId(), Instant.now(),
                actor.userId(), 0);
        Proveedor saved = proveedores.crear(nuevo, actor.userId());
        audit(actor, "CREAR", saved.id());
        return saved;
    }

    private void validarDatos(ProveedorCommand command) {
        if (command.nombre() == null || command.nombre().isBlank()) {
            throw new BusinessException(ErrorCode.PROVEEDOR_DATOS_REQUERIDOS);
        }
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Proveedor require(UUID id) {
        return proveedores.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.PROVEEDOR_NOT_FOUND));
    }

    private void audit(CurrentUser u, String accion, UUID id) {
        events.publishEvent(new ProveedorAuditEvent(u.empresaId(), u.userId(), accion, id, Instant.now()));
    }
}
