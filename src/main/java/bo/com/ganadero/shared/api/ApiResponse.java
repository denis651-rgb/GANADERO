package bo.com.ganadero.shared.api;

import java.time.Instant;

public record ApiResponse<T>(
        boolean ok,
        T data,
        Instant timestamp,
        String correlationId
) {
    public static <T> ApiResponse<T> success(T data, String correlationId) {
        return new ApiResponse<>(true, data, Instant.now(), correlationId);
    }
}
