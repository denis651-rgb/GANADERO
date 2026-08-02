package bo.com.ganadero.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        boolean ok,
        String code,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp,
        String correlationId
) {
    public record FieldError(String field, String message) {
    }
}
