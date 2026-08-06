package bo.com.ganadero.shared.api;

import java.time.Instant;
import java.util.List;

public record ApiError(
        boolean ok,
        String code,
        String message,
        List<FieldError> fieldErrors,
        Instant timestamp,
        String correlationId,
        Long localVersion,
        Long serverVersion,
        Object serverData,
        List<String> conflictingFields,
        String suggestedAction
) {
    public ApiError(boolean ok, String code, String message, List<FieldError> fieldErrors,
                    Instant timestamp, String correlationId) {
        this(ok, code, message, fieldErrors, timestamp, correlationId, null, null, null, null, null);
    }

    public record FieldError(String field, String message) {
    }
}
