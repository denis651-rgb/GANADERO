package bo.com.ganadero.auditoria.application;

import bo.com.ganadero.auditoria.domain.AuditoriaRegistro;
import bo.com.ganadero.auditoria.domain.AuditoriaRepository;
import bo.com.ganadero.animales.application.AnimalAuditEvent;
import bo.com.ganadero.archivos.application.ArchivoAuditEvent;
import bo.com.ganadero.lotes.application.LoteAuditEvent;
import bo.com.ganadero.movimientos.application.MovimientoAuditEvent;
import bo.com.ganadero.pesajes.application.PesajeAuditEvent;
import bo.com.ganadero.potreros.application.PotreroAuditEvent;
import bo.com.ganadero.propiedades.application.CampoAuditEvent;
import bo.com.ganadero.seguridad.application.SeguridadAuditEvent;
import bo.com.ganadero.shared.audit.EmpresaAuditEvent;
import bo.com.ganadero.shared.audit.SyncAuditEvent;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class AuditEventListener {
    private final AuditoriaRepository repository;

    public AuditEventListener(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAuditLog(AuditLogEvent event) {
        RequestInfo request = requestInfo();
        repository.insert(new AuditoriaRegistro(
                UUID.randomUUID(), event.empresaId(), event.usuarioId(), event.accion(), event.modulo(),
                event.entidad(), event.entidadId(), request.correlationId(), "EXITO",
                Map.of(), event.datosAnteriores(), event.datosNuevos(),
                request.dispositivo(), request.ip(), request.userAgent(),
                event.occurredAt() == null ? Instant.now() : event.occurredAt()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnimal(AnimalAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "ANIMALES", event.entidad(), event.entidadId(), event.datos());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCampo(CampoAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "PROPIEDADES", event.entidad(), event.entidadId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPotrero(PotreroAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "POTREROS", event.entidad(), event.entidadId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSeguridad(SeguridadAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "SEGURIDAD", event.entidadTipo(), event.entidadId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLote(LoteAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "LOTES", event.entidad(), event.entidadId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMovimiento(MovimientoAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "MOVIMIENTOS", event.entidad(), event.entidadId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPesaje(PesajeAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "PESAJE", event.entidad(), event.entidadId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArchivo(ArchivoAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "ARCHIVOS", event.entidad(), event.entidadId(), event.datos());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmpresa(EmpresaAuditEvent event) {
        if (event.empresaId() == null) return;
        RequestInfo request = requestInfo();
        repository.insert(new AuditoriaRegistro(
                UUID.randomUUID(), event.empresaId(), event.usuarioId(), "ACTUALIZAR", "EMPRESAS",
                event.entidadTipo(), event.entidadId(), request.correlationId(), "EXITO",
                Map.of(), Map.of(), Map.of(),
                request.dispositivo(), request.ip(), request.userAgent(), event.fecha()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSync(SyncAuditEvent event) {
        persist(event.empresaId(), event.usuarioId(), event.accion(), "SINCRONIZACION", event.entidad(),
                event.entidadId(), event.datos());
    }

    private void persist(UUID empresaId, UUID usuarioId, String accion, String modulo, String entidad, UUID entidadId) {
        persist(empresaId, usuarioId, accion, modulo, entidad, entidadId, Map.of());
    }

    private void persist(UUID empresaId, UUID usuarioId, String accion, String modulo, String entidad, UUID entidadId, Map<String, Object> datos) {
        if (empresaId == null) return;
        RequestInfo request = requestInfo();
        repository.insert(new AuditoriaRegistro(
                UUID.randomUUID(), empresaId, usuarioId, accion, modulo, entidad, entidadId,
                request.correlationId(), "EXITO", datos, Map.of(), Map.of(),
                request.dispositivo(), request.ip(), request.userAgent(), Instant.now()));
    }

    private RequestInfo requestInfo() {
        HttpServletRequest request = null;
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                request = attrs.getRequest();
            }
        } catch (IllegalStateException ignored) {
            // sin request activo (evento publicado fuera de una petición HTTP)
        }
        if (request == null) return new RequestInfo(null, null, null, null);
        Object correlation = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        String userAgent = request.getHeader("User-Agent");
        String dispositivo = dispositivosMobiles(userAgent);
        return new RequestInfo(correlation == null ? null : correlation.toString(),
                request.getRemoteAddr(), userAgent, dispositivo);
    }

    private String dispositivosMobiles(String userAgent) {
        if (userAgent == null) return "WEB";
        String value = userAgent.toLowerCase();
        if (value.contains("android")) return "ANDROID";
        if (value.contains("iphone") || value.contains("ipad")) return "IOS";
        return "WEB";
    }

    private record RequestInfo(String correlationId, String ip, String userAgent, String dispositivo) {}
}
