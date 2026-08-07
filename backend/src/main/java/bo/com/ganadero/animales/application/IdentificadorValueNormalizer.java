package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.TipoIdentificador;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class IdentificadorValueNormalizer {

    public String normalize(TipoIdentificador tipo, String valor) {
        if (tipo == null || valor == null) {
            throw new BusinessException(ErrorCode.IDENTIFIER_INVALID_VALUE);
        }
        String value = valor.trim();
        switch (tipo) {
            case ARETE -> value = normalizeArete(value);
            case RFID -> value = normalizeRfid(value);
            case TATUAJE -> value = normalizeTatuaje(value);
            case OTRO -> value = normalizeOtro(value);
            case QR -> throw new BusinessException(ErrorCode.IDENTIFIER_QR_MANUAL_NOT_ALLOWED);
        }
        if (value.isEmpty() || value.length() > 120) {
            throw new BusinessException(ErrorCode.IDENTIFIER_INVALID_VALUE);
        }
        return value;
    }

    private String normalizeArete(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        requirePattern(upper, "[A-Z0-9-]+");
        return upper;
    }

    private String normalizeRfid(String value) {
        String compact = value.replaceAll("\\s+", "");
        String upper = compact.toUpperCase(Locale.ROOT);
        requirePattern(upper, "[0-9A-F]+");
        return upper;
    }

    private String normalizeTatuaje(String value) {
        String upper = value.toUpperCase(Locale.ROOT);
        requirePattern(upper, "[A-Z0-9 /-]+");
        return upper;
    }

    private String normalizeOtro(String value) {
        return value;
    }

    private void requirePattern(String value, String pattern) {
        if (!value.matches(pattern)) {
            throw new BusinessException(ErrorCode.IDENTIFIER_INVALID_VALUE);
        }
    }
}
