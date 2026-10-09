package bo.com.ganadero.integraciones.telegram;

import bo.com.ganadero.alertas.application.AlertaCanceladaEvent;
import bo.com.ganadero.alertas.application.AlertaProgramadaEvent;
import bo.com.ganadero.alertas.application.TipoAlerta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class AlertaTelegramEventListener {

    private static final Logger log = LoggerFactory.getLogger(AlertaTelegramEventListener.class);

    private final OutboxAlertaRepository outboxRepository;
    private final FirebaseRelaySyncService syncService;

    public AlertaTelegramEventListener(
            OutboxAlertaRepository outboxRepository,
            FirebaseRelaySyncService syncService) {
        this.outboxRepository = outboxRepository;
        this.syncService = syncService;
    }

    @EventListener
    public void onAlertaProgramada(AlertaProgramadaEvent event) {
        try {
            if (!esNotificableTelegram(event.tipo())) {
                log.debug("Alerta de tipo {} excluida de Telegram (se mantiene en la web)", event.tipo());
                return;
            }

            String tipoModulo = clasificarModulo(event.tipo());
            String animalCodigo = extraerCodigoAnimal(event.metadata());

            UUID empresaId = event.empresaId() != null
                    ? event.empresaId()
                    : UUID.fromString("00000000-0000-0000-0000-000000000001");

            OutboxAlerta outbox = new OutboxAlerta(
                    event.id(),
                    empresaId,
                    tipoModulo,
                    event.tipo().name(),
                    event.animalId(),
                    animalCodigo,
                    event.titulo(),
                    event.mensaje(),
                    event.fechaProgramada(),
                    event.severidad() != null ? event.severidad() : "MEDIA",
                    OutboxAlerta.ESTADO_PENDIENTE_SUBIDA,
                    0,
                    null,
                    event.origenTipo(),
                    event.origenId(),
                    Instant.now(),
                    null
            );

            outboxRepository.encolar(outbox);
            log.debug("Alerta encolada en Outbox Cloud Relay: {}", event.id());

            if (syncService.isConectado()) {
                intentarSincronizacionAsincrona();
            }
        } catch (Exception e) {
            log.warn("Error al registrar alerta en Outbox Cloud Relay: {}", e.getMessage());
        }
    }

    @EventListener
    public void onAlertaCancelada(AlertaCanceladaEvent event) {
        try {
            outboxRepository.cancelarPorOrigen(event.empresaId(), event.origenTipo(), event.origenId());
            if (syncService.isConectado()) {
                intentarSincronizacionAsincrona();
            }
        } catch (Exception e) {
            log.warn("Error al cancelar alerta en Outbox Cloud Relay: {}", e.getMessage());
        }
    }

    @Async
    public void intentarSincronizacionAsincrona() {
        try {
            syncService.sincronizarPendientes();
        } catch (Exception ignored) {
        }
    }

    private String clasificarModulo(TipoAlerta tipo) {
        if (tipo == null) return "GENERAL";
        String nombreTipo = tipo.name();
        if (nombreTipo.contains("VACUNA") || nombreTipo.contains("SANITARI") || nombreTipo.contains("CLINICO")
                || nombreTipo.contains("REVISION") || nombreTipo.contains("TRATAMIENTO")) {
            return "SANIDAD";
        }
        if (nombreTipo.contains("PARTO") || nombreTipo.contains("CELO") || nombreTipo.contains("DIAGNOSTICO")
                || nombreTipo.contains("GESTACION") || nombreTipo.contains("REPRODUCT")) {
            return "REPRODUCCION";
        }
        return "GENERAL";
    }

    private boolean esNotificableTelegram(TipoAlerta tipo) {
        if (tipo == null) return false;
        return switch (tipo) {
            case PARTO_PROXIMO, CELO_DETECTADO, DIAGNOSTICO_PENDIENTE, DESTETE_PROXIMO,
                 VACUNA_PROXIMA, VACUNA_VENCIDA, ACTIVIDAD_SANITARIA_PROXIMA, ACTIVIDAD_SANITARIA_VENCIDA,
                 RETIRO_CARNE_VIGENTE, RETIRO_LECHE_VIGENTE, TRATAMIENTO_PROXIMO, TRATAMIENTO_ATRASADO,
                 CASO_CLINICO_CRITICO, RECORDATORIO_SANIDAD -> true;
            default -> false;
        };
    }

    private String extraerCodigoAnimal(Map<String, Object> metadata) {
        if (metadata == null) return "";
        Object cod = metadata.get("animalCodigo");
        if (cod != null && !cod.toString().isBlank()) return cod.toString().trim();
        Object nom = metadata.get("animalNombre");
        if (nom != null && !nom.toString().isBlank()) return nom.toString().trim();
        return "";
    }
}
