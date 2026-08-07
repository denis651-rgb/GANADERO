package bo.com.ganadero.animales.qr;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class QrPayloadService {
    private static final String ALGORITHM = "HmacSHA256";

    private final QrProperties properties;
    private final ObjectMapper objectMapper;
    private final SecretKeySpec key;

    public QrPayloadService(QrProperties properties, ObjectMapper objectMapper) {
        if (properties == null || properties.signingSecret() == null || properties.signingSecret().isBlank()) {
            throw new IllegalStateException("app.qr.signing-secret no está configurado.");
        }
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.key = new SecretKeySpec(properties.signingSecret().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public AnimalQrPayload sign(UUID animalId, UUID identifierId) {
        int version = properties.payloadVersion();
        String canonical = canonical(animalId, identifierId, version);
        return new AnimalQrPayload(AnimalQrPayload.TYPE, animalId, identifierId, version, hmac(canonical));
    }

    public String canonical(UUID animalId, UUID identifierId, int version) {
        return AnimalQrPayload.TYPE + "|" + animalId + "|" + identifierId + "|" + version;
    }

    public String toJson(AnimalQrPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException exception) {
            throw new IllegalStateException("No se pudo serializar el payload QR", exception);
        }
    }

    public AnimalQrPayload parse(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) throw invalid();
            AnimalQrPayload payload = new AnimalQrPayload(text(node.get("type")), uuid(node.get("animalId")),
                    uuid(node.get("identifierId")), node.path("version").asInt(-1), text(node.get("signature")));
            if (!payload.hasValidStructure()) throw invalid();
            return payload;
        } catch (JacksonException exception) {
            throw invalid();
        } catch (RuntimeException exception) {
            throw invalid();
        }
    }

    public boolean verify(AnimalQrPayload payload) {
        if (payload == null || !payload.hasValidStructure()) return false;
        if (payload.version() != properties.payloadVersion()) return false;
        String expected = hmac(canonical(payload.animalId(), payload.identifierId(), payload.version()));
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                payload.signature().getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo firmar el QR", exception);
        }
    }

    private BusinessException invalid() {
        return new BusinessException(ErrorCode.INVALID_QR);
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private UUID uuid(JsonNode node) {
        if (node == null || node.isNull()) return null;
        try {
            return UUID.fromString(node.asText());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
