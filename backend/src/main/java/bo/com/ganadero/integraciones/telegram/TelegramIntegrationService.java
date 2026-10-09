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
import java.util.List;
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
            "🐮 <b>Prueba de Conexión - GANADERO</b>\n\n" +
            "✅ ¡El Bot de Telegram se ha configurado exitosamente!\n\n" +
            "Las alertas de <b>Sanidad</b>, <b>Vacunaciones</b>, <b>Celos</b> y <b>Partos programados</b> se notificarán por este medio.\n\n" +
            "📅 <i>Fecha de prueba:</i> <code>%s</code>\n" +
            "🖥 <i>Origen:</i> <code>App de Escritorio GANADERO</code>",
            escapeHtml(FORMATO_FECHA.format(Instant.now()))
        );

        return enviarTextoATelegram(botToken, chatId, texto, true);
    }

    public ResultadoEnvioTelegram enviarAlertaEjemplo(UUID empresaId, String tipoEjemplo) {
        ConfiguracionTelegram config = consultar(empresaId);
        validarConfiguracion(config);

        String texto;
        String fechaHoy = escapeHtml(FORMATO_FECHA.format(Instant.now()));

        if ("PARTO".equalsIgnoreCase(tipoEjemplo)) {
            texto = String.format(
                "🐮 <b>ALERTA REPRODUCTIVA: Parto Estimado</b>\n\n" +
                "🏷 <b>Hembra:</b> <code>VACA-204</code> (Lote Maternidad)\n" +
                "🤰 <b>Evento:</b> Cumplimiento de 283 días de gestación\n" +
                "📅 <b>Fecha:</b> <code>%s</code>\n" +
                "⚠️ <b>Recomendación:</b> Trasladar al corral de parición y alistar kit neonatal.\n\n" +
                "<i>Sistema GANADERO Desktop (Alerta Real)</i>",
                fechaHoy
            );
        } else if ("TRATAMIENTO".equalsIgnoreCase(tipoEjemplo)) {
            texto = String.format(
                "💊 <b>ALERTA DE TRATAMIENTO: Aplicación Pendiente</b>\n\n" +
                "🏷 <b>Animal:</b> <code>TORO-05</code> (Potrero 3)\n" +
                "💊 <b>Medicamento:</b> Oxitetraciclina L.A. (Segunda dosis)\n" +
                "📅 <b>Fecha:</b> <code>%s</code>\n" +
                "⛔ <b>Retiro en Carne:</b> 28 días de restricción activa\n\n" +
                "<i>Sistema GANADERO Desktop (Alerta Real)</i>",
                fechaHoy
            );
        } else {
            texto = String.format(
                "💉 <b>ALERTA SANITARIA: Vacunación Programada</b>\n\n" +
                "🏷 <b>Animal:</b> <code>VACA-1042</code> (Hato Reproductoras)\n" +
                "💉 <b>Vacuna:</b> Fiebre Aftosa (Bivalente O/A)\n" +
                "📅 <b>Fecha Programada:</b> <code>%s</code>\n" +
                "⚠️ <b>Dosis:</b> 2 ml vía subcutánea en tabla del cuello\n\n" +
                "<i>Sistema GANADERO Desktop (Alerta Real)</i>",
                fechaHoy
            );
        }

        return enviarTextoATelegram(config.botToken(), config.chatIdPrincipal(), texto, true);
    }

    public int despacharAlertasReales(UUID empresaId) {
        ConfiguracionTelegram config = consultar(empresaId);
        validarConfiguracion(config);

        List<OutboxAlerta> alertas = outboxRepository.buscarPendientes(15);
        if (alertas.isEmpty()) {
            return 0;
        }

        int enviadas = 0;
        for (OutboxAlerta alerta : alertas) {
            if (alerta.subtipo() != null && (alerta.subtipo().contains("CUARENTENA") || alerta.subtipo().contains("REVISION_SANITARIA_INGRESO") || alerta.subtipo().contains("MOVIMIENTO") || alerta.subtipo().contains("PESAJE"))) {
                outboxRepository.marcarSincronizado(alerta.id());
                continue;
            }

            String fechaTexto = alerta.fechaProgramada() != null
                    ? FORMATO_FECHA.format(alerta.fechaProgramada())
                    : FORMATO_FECHA.format(Instant.now());
            String codigoAnimal = alerta.animalCodigo() != null && !alerta.animalCodigo().isBlank()
                    ? alerta.animalCodigo()
                    : "Hato General";
            String modulo = alerta.tipoModulo() != null ? alerta.tipoModulo() : "SANIDAD";
            String subtipo = alerta.subtipo() != null ? alerta.subtipo() : "AVISO";
            String titulo = alerta.titulo() != null ? alerta.titulo() : "Aviso Ganadero";
            String mensaje = alerta.mensaje() != null ? alerta.mensaje() : "Actividad sanitaria o reproductiva programada.";

            String texto = String.format(
                "<b>%s</b>\n\n" +
                "📅 <b>Fecha:</b> <code>%s</code>\n" +
                "🏷 <b>Animal:</b> <code>%s</code>\n" +
                "📋 <b>Módulo:</b> %s (%s)\n\n" +
                "%s\n\n" +
                "<i>Sistema GANADERO Desktop</i>",
                escapeHtml(titulo),
                escapeHtml(fechaTexto),
                escapeHtml(codigoAnimal),
                escapeHtml(modulo),
                escapeHtml(subtipo),
                escapeHtml(mensaje)
            );

            ResultadoEnvioTelegram res = enviarTextoATelegram(config.botToken(), config.chatIdPrincipal(), texto, true);
            if (res.exito()) {
                outboxRepository.marcarSincronizado(alerta.id());
                enviadas++;
            }
        }
        return enviadas;
    }

    private void validarConfiguracion(ConfiguracionTelegram config) {
        if (config.botToken() == null || config.botToken().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        if (config.chatIdPrincipal() == null || config.chatIdPrincipal().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private ResultadoEnvioTelegram enviarTextoATelegram(String botToken, String chatId, String texto, boolean usarHtml) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", texto);
            if (usarHtml) {
                body.put("parse_mode", "HTML");
            }

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
                return new ResultadoEnvioTelegram(true, "Notificación enviada exitosamente a Telegram.", 200);
            } else {
                String desc = resp.body();
                try {
                    JsonObject root = JsonParser.parseString(resp.body()).getAsJsonObject();
                    if (root.has("description")) {
                        desc = root.get("description").getAsString();
                    }
                } catch (Exception ignored) {}

                // Si falló por parsing de entidades HTML, reintentar automáticamente en texto plano sin parse_mode
                if (usarHtml && desc != null && desc.toLowerCase().contains("can't parse entities")) {
                    log.warn("Reintentando envío a Telegram en texto plano debido a error de parseo: {}", desc);
                    String textoPlano = texto.replaceAll("<[^>]*>", "");
                    return enviarTextoATelegram(botToken, chatId, textoPlano, false);
                }

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
