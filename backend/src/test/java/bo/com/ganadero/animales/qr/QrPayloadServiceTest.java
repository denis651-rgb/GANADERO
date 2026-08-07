package bo.com.ganadero.animales.qr;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrPayloadServiceTest {
    private final QrProperties properties = new QrProperties("test-qr-secret", 1, 2048, 20);
    private final QrPayloadService service = new QrPayloadService(properties, new ObjectMapper());

    @Test
    void signsAndVerifiesValidPayload() {
        UUID animalId = UUID.randomUUID();
        UUID identifierId = UUID.randomUUID();
        AnimalQrPayload payload = service.sign(animalId, identifierId);
        assertThat(payload.type()).isEqualTo(AnimalQrPayload.TYPE);
        assertThat(payload.version()).isEqualTo(1);
        assertThat(payload.signature()).hasSize(64);
        assertThat(service.verify(payload)).isTrue();
    }

    @Test
    void canonicalStringIsStableAndPredictable() {
        UUID animalId = UUID.randomUUID();
        UUID identifierId = UUID.randomUUID();
        assertThat(service.canonical(animalId, identifierId, 1))
                .isEqualTo("GANADERO_ANIMAL|" + animalId + "|" + identifierId + "|1");
    }

    @Test
    void verifyRejectsTamperedSignature() {
        AnimalQrPayload payload = service.sign(UUID.randomUUID(), UUID.randomUUID());
        AnimalQrPayload tampered = new AnimalQrPayload(payload.type(), payload.animalId(), payload.identifierId(),
                payload.version(), "0".repeat(64));
        assertThat(service.verify(tampered)).isFalse();
    }

    @Test
    void verifyRejectsTamperedAnimalId() {
        AnimalQrPayload payload = service.sign(UUID.randomUUID(), UUID.randomUUID());
        AnimalQrPayload changed = new AnimalQrPayload(payload.type(), UUID.randomUUID(), payload.identifierId(),
                payload.version(), payload.signature());
        assertThat(service.verify(changed)).isFalse();
    }

    @Test
    void verifyRejectsUnsupportedVersion() {
        AnimalQrPayload payload = new AnimalQrPayload(AnimalQrPayload.TYPE, UUID.randomUUID(), UUID.randomUUID(),
                99, "a".repeat(64));
        assertThat(service.verify(payload)).isFalse();
    }

    @Test
    void parseRoundTripsThroughJson() {
        AnimalQrPayload payload = service.sign(UUID.randomUUID(), UUID.randomUUID());
        AnimalQrPayload parsed = service.parse(service.toJson(payload));
        assertThat(parsed).isEqualTo(payload);
        assertThat(service.verify(parsed)).isTrue();
    }

    @Test
    void parseRejectsMalformedOrIncompletePayloads() {
        assertThatThrownBy(() -> service.parse("not-json{"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_QR));
        assertThatThrownBy(() -> service.parse("{}"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_QR));
        assertThatThrownBy(() -> service.parse("{\"type\":\"GANADERO_ANIMAL\"}"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_QR));
        assertThatThrownBy(() -> service.parse("{\"type\":\"OTRO\",\"animalId\":\"" + UUID.randomUUID()
                + "\",\"identifierId\":\"" + UUID.randomUUID() + "\",\"version\":1,\"signature\":\"" + "a".repeat(64) + "\"}"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_QR));
    }

    @Test
    void constructorRequiresSigningSecret() {
        assertThatThrownBy(() -> new QrPayloadService(new QrProperties("", 1, 2048, 20), new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new QrPayloadService(null, new ObjectMapper()))
                .isInstanceOf(IllegalStateException.class);
    }
}
