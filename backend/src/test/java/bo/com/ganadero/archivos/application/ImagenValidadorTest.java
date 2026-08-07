package bo.com.ganadero.archivos.application;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenValidadorTest {

    private static final byte[] PNG_1x1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
    private static final byte[] WEBP_1x1 = Base64.getDecoder().decode(
            "UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==");

    private final ImagenValidador validador = new ImagenValidador();

    @Test
    void acceptsValidPng() {
        var result = validador.validar(PNG_1x1, "image/png");
        assertThat(result.formato()).isEqualTo("image/png");
        assertThat(result.ancho()).isEqualTo(1);
        assertThat(result.alto()).isEqualTo(1);
    }

    @Test
    void acceptsValidWebp() {
        var result = validador.validar(WEBP_1x1, "image/webp");
        assertThat(result.formato()).isEqualTo("image/webp");
        assertThat(result.ancho()).isEqualTo(1);
        assertThat(result.alto()).isEqualTo(1);
    }

    @Test
    void rejectsRenamedExecutable() {
        byte[] exe = "MZ\u0000\u0000this is not an image at all".getBytes();
        assertThatThrownBy(() -> validador.validar(exe, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_FILE_INVALID));
    }

    @Test
    void rejectsMimeMismatch() {
        assertThatThrownBy(() -> validador.validar(PNG_1x1, "image/webp"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_FILE_INVALID));
    }

    @Test
    void rejectsCorruptImage() {
        byte[] corrupt = new byte[PNG_1x1.length];
        System.arraycopy(PNG_1x1, 0, corrupt, 0, corrupt.length);
        corrupt[40] = (byte) 0x7F;
        corrupt[41] = (byte) 0x7F;
        corrupt[42] = (byte) 0x7F;
        assertThatThrownBy(() -> validador.validar(corrupt, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_FILE_INVALID));
    }

    @Test
    void rejectsTooSmallContent() {
        assertThatThrownBy(() -> validador.validar(new byte[]{1, 2, 3}, "image/png"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_FILE_INVALID));
    }
}
