package bo.com.ganadero.integraciones.telegram;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TelegramIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(TelegramIntegrationService.class);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZONA_NEGOCIO);

    private final TelegramConfiguracionRepository configRepository;
    private final OutboxAlertaRepository outboxRepository;
    private final FirebaseRelaySyncService firebaseSyncService;
    private final Gson gson;
    private final HttpClient httpClient;

    public TelegramIntegrationService(
            TelegramConfiguracionRepository configRepository,
            OutboxAlertaRepository outboxRepository,
            FirebaseRelaySyncService firebaseSyncService) {
        this.configRepository = configRepository;
        this.outboxRepository = outboxRepository;
        this.firebaseSyncService = firebaseSyncService;
        this.gson = new Gson();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Transactional(readOnly = true)
    public ConfiguracionTelegram consultar(UUID empresaId) {
        return configRepository.buscarPorEmpresa(empresaId)
                .orElseGet(() -> ConfiguracionTelegram.porDefecto(empresaId));
    }

    @Transactional
    public ConfiguracionTelegram guardar(UUID empresaId, GuardarTelegramConfigRequest request) {
        ConfiguracionTelegram actual = consultar(empresaId);
        ConfiguracionTelegram actualizada = new ConfiguracionTelegram(
                actual.id(),
                empresaId,
                request.botToken() != null ? request.botToken().trim() : "",
                request.chatIdPrincipal() != null ? request.chatIdPrincipal().trim() : "",
                request.chatIdsJson() != null ? request.chatIdsJson().trim() : "[]",
                request.activo() != null ? request.activo() : true,
                request.notificarSanidad() != null ? request.notificarSanidad() : true,
                request.notificarReproduccion() != null ? request.notificarReproduccion() : true,
                request.horaNotificacion() != null ? request.horaNotificacion().trim() : "07:00",
                actual.createdAt(),
                Instant.now(),
                actual.version()
        );

        configRepository.guardar(actualizada);
        firebaseSyncService.sincronizarConfiguracionTelegram(actualizada);
        return actualizada;
    }

    public ResultadoEnvioTelegram enviarMensajePrueba(UUID empresaId, EnviarPruebaRequest request) {
        ConfiguracionTelegram config = consultar(empresaId);
        String botToken = (request != null && request.botToken() != null && !request.botToken().isBlank())
                ? request.botToken().trim() : config.botToken();
        String chatId = (request != null && request.chatId() != null && !request.chatId().isBlank())
                ? request.chatId().trim() : config.chatIdPrincipal();

        if (botToken == null || botToken.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (chatId == null || chatId.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }

        String texto = String.format(
            "🐮 *Prueba de Conexión - GANADERO*\n\n" +
            "✅ ¡El Bot de Telegram se ha configurado exitosamente!\n\n" +
            "Las alertas de *Sanidad*, *Vacunaciones*, *Celos* y *Partos programados* se notificarán por este medio.\n\n" +
            "📅 _Fecha de prueba:_ `%s`\n" +
            "🖥 _Origen:_ `App de Escritorio GANADERO`",
            FORMATO_FECHA.format(Instant.now())
        );

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", texto);
            body.put("parse_mode", "Markdown");

            String jsonPayload = gson.toJson(body);
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 200) {
                return new ResultadoEnvioTelegram(true, "Mensaje enviado exitosamente a Telegram.", 200);
            } else {
                String desc = resp.body();
                try {
                    JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
                    if (root.has("description")) {
                        desc = root.get("description").getAsString();
                    }
                } catch (Exception ignored) {}
                log.warn("Telegram API error {}: {}", resp.statusCode(), desc);
                return new ResultadoEnvioTelegram(false, "Telegram API error: " + desc, resp.statusCode());
            }
        } catch (Exception e) {
            log.error("Excepción al conectar con Telegram API", e);
            return new ResultadoEnvioTelegram(false, "Error de red al conectar con Telegram: " + e.getMessage(), 500);
        }
    }

    public EstadoIntegracionTelegram consultarEstado(UUID empresaId) {
        ConfiguracionTelegram config = consultar(empresaId);
        int pendientes = outboxRepository.contarPendientes(empresaId);
        boolean firebaseConectado = firebaseSyncService.isConectado();
        boolean telegramConfigurado = config.botToken() != null && !config.botToken().isBlank()
                && config.chatIdPrincipal() != null && !config.chatIdPrincipal().isBlank();

        return new EstadoIntegracionTelegram(
                config.activo(),
                telegramConfigurado,
                firebaseConectado,
                pendientes,
                config.horaNotificacion(),
                config.notificarSanidad(),
                config.notificarReproduccion()
        );
    }

    public record GuardarTelegramConfigRequest(
            String botToken,
            String chatIdPrincipal,
            String chatIdsJson,
            Boolean activo,
            Boolean notificarSanidad,
            Boolean notificarReproduccion,
            String horaNotificacion
    ) {}

    public record EnviarPruebaRequest(String botToken, String chatId) {}

    public record ResultadoEnvioTelegram(boolean exito, String mensaje, int httpStatus) {}

    public record EstadoIntegracionTelegram(
            boolean activo,
            boolean telegramConfigurado,
            boolean firebaseConectado,
            int alertasPendientesSincronizacion,
            String horaNotificacion,
            boolean notificarSanidad,
            boolean notificarReproduccion
    ) {}
}
