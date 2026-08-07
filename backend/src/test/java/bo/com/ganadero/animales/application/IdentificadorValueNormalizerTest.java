package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.TipoIdentificador;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentificadorValueNormalizerTest {
    private final IdentificadorValueNormalizer normalizer = new IdentificadorValueNormalizer();

    @Test
    void normalizesArete() {
        assertThat(normalizer.normalize(TipoIdentificador.ARETE, " ar-001 ")).isEqualTo("AR-001");
        assertThat(normalizer.normalize(TipoIdentificador.ARETE, "abc-123")).isEqualTo("ABC-123");
    }

    @Test
    void rejectsAreteWithInvalidChars() {
        assertThatThrownBy(() -> normalizer.normalize(TipoIdentificador.ARETE, "AR 001"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_INVALID_VALUE));
        assertThatThrownBy(() -> normalizer.normalize(TipoIdentificador.ARETE, "AR_001"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_INVALID_VALUE));
    }

    @Test
    void normalizesRfid() {
        assertThat(normalizer.normalize(TipoIdentificador.RFID, " 12 34 ab ")).isEqualTo("1234AB");
        assertThat(normalizer.normalize(TipoIdentificador.RFID, "840123456789013")).isEqualTo("840123456789013");
    }

    @Test
    void rejectsRfidWithNonHexChars() {
        assertThatThrownBy(() -> normalizer.normalize(TipoIdentificador.RFID, "XYZ"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_INVALID_VALUE));
    }

    @Test
    void normalizesTatuaje() {
        assertThat(normalizer.normalize(TipoIdentificador.TATUAJE, " p-12 a ")).isEqualTo("P-12 A");
    }

    @Test
    void preservesOtroCase() {
        assertThat(normalizer.normalize(TipoIdentificador.OTRO, "  R-2b  ")).isEqualTo("R-2b");
    }

    @Test
    void rejectsBlankValue() {
        assertThatThrownBy(() -> normalizer.normalize(TipoIdentificador.ARETE, "   "))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_INVALID_VALUE));
    }

    @Test
    void rejectsManualQr() {
        assertThatThrownBy(() -> normalizer.normalize(TipoIdentificador.QR, "any"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_QR_MANUAL_NOT_ALLOWED));
    }
}
