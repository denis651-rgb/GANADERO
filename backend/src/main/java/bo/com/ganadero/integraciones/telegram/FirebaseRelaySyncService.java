package bo.com.ganadero.integraciones.telegram;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.SetOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "ganadero.firebase", name = "sync-enabled", havingValue = "true", matchIfMissing = true)
public class FirebaseRelaySyncService {

    private static final Logger log = LoggerFactory.getLogger(FirebaseRelaySyncService.class);

    private final OutboxAlertaRepository outboxRepository;
    private final TelegramConfiguracionRepository telegramConfigRepository;
    private final Optional<Firestore> firestoreClient;

    public FirebaseRelaySyncService(
            OutboxAlertaRepository outboxRepository,
            TelegramConfiguracionRepository telegramConfigRepository,
            Optional<Firestore> firestoreClient) {
        this.outboxRepository = outboxRepository;
        this.telegramConfigRepository = telegramConfigRepository;
        this.firestoreClient = firestoreClient;
    }

    public boolean isConectado() {
        return firestoreClient.isPresent();
    }

    @Scheduled(fixedDelayString = "${ganadero.firebase.sync-delay-ms:60000}")
    public void procesarColaSincronizacion() {
        if (firestoreClient.isEmpty()) {
            return;
        }
        sincronizarPendientes();
    }

    public int sincronizarPendientes() {
        if (firestoreClient.isEmpty()) {
            log.debug("Firebase Relay: No hay cliente Firestore configurado. Omitiendo sincronización.");
            return 0;
        }

        Firestore db = firestoreClient.get();
        List<OutboxAlerta> pendientes = outboxRepository.buscarPendientes(50);
        if (pendientes.isEmpty()) {
            return 0;
        }

        int sincronizados = 0;
        for (OutboxAlerta alerta : pendientes) {
            try {
                if (OutboxAlerta.ESTADO_PENDIENTE_SUBIDA.equals(alerta.estadoSincronizacion())) {
                    Map<String, Object> doc = new HashMap<>();
                    doc.put("id", alerta.id().toString());
                    doc.put("cuenta_id", alerta.empresaId().toString());
                    doc.put("tipo_modulo", alerta.tipoModulo());
                    doc.put("subtipo", alerta.subtipo());
                    doc.put("animal_codigo", alerta.animalCodigo() != null ? alerta.animalCodigo() : "");
                    doc.put("animal_id", alerta.animalId() != null ? alerta.animalId().toString() : "");
                    doc.put("titulo", alerta.titulo());
                    doc.put("mensaje", alerta.mensaje());
                    doc.put("fecha_programada", alerta.fechaProgramada().toString());
                    doc.put("prioridad", alerta.prioridad());
                    doc.put("estado", "PENDIENTE");
                    doc.put("origen_tipo", alerta.origenTipo() != null ? alerta.origenTipo() : "");
                    doc.put("origen_id", alerta.origenId() != null ? alerta.origenId().toString() : "");
                    doc.put("creado_en", alerta.creadoEn().toString());
                    doc.put("actualizado_en", Instant.now().toString());

                    db.collection("recordatorios")
                            .document(alerta.id().toString())
                            .set(doc, SetOptions.merge())
                            .get(); // Espera confirmación de escritura

                    outboxRepository.marcarSincronizado(alerta.id());
                    sincronizados++;
                } else if (OutboxAlerta.ESTADO_PENDIENTE_CANCELACION.equals(alerta.estadoSincronizacion())) {
                    Map<String, Object> cancelDoc = new HashMap<>();
                    cancelDoc.put("estado", "CANCELADO");
                    cancelDoc.put("cancelado_en", Instant.now().toString());

                    db.collection("recordatorios")
                            .document(alerta.id().toString())
                            .set(cancelDoc, SetOptions.merge())
                            .get();

                    outboxRepository.marcarCancelado(alerta.id());
                    sincronizados++;
                }
            } catch (Exception e) {
                log.warn("Firebase Relay: Error al sincronizar recordatorio {} con la nube: {}", alerta.id(), e.getMessage());
                outboxRepository.registrarFallo(alerta.id(), e.getMessage(), alerta.intentos() + 1);
                break; // Pausar lote si se perdió la conexión
            }
        }

        if (sincronizados > 0) {
            log.info("Firebase Relay: Se sincronizaron {} recordatorios con éxito en la nube.", sincronizados);
        }
        return sincronizados;
    }

    public void sincronizarConfiguracionTelegram(ConfiguracionTelegram config) {
        if (firestoreClient.isEmpty()) {
            return;
        }
        try {
            Firestore db = firestoreClient.get();
            Map<String, Object> doc = new HashMap<>();
            doc.put("cuenta_id", config.empresaId().toString());
            doc.put("bot_token", config.botToken() != null ? config.botToken() : "");
            doc.put("chat_id_principal", config.chatIdPrincipal() != null ? config.chatIdPrincipal() : "");
            doc.put("chat_ids_json", config.chatIdsJson() != null ? config.chatIdsJson() : "[]");
            doc.put("activo", config.activo());
            doc.put("notificar_sanidad", config.notificarSanidad());
            doc.put("notificar_reproduccion", config.notificarReproduccion());
            doc.put("hora_notificacion", config.horaNotificacion() != null ? config.horaNotificacion() : "07:00");
            doc.put("actualizado_en", Instant.now().toString());

            db.collection("configuracion_telegram")
                    .document(config.empresaId().toString())
                    .set(doc, SetOptions.merge())
                    .get();

            log.info("Firebase Relay: Configuración de Telegram actualizada en Firestore para empresa {}.", config.empresaId());
        } catch (Exception e) {
            log.warn("Firebase Relay: No se pudo sincronizar la configuración de Telegram en la nube: {}", e.getMessage());
        }
    }
}
