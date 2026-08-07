package bo.com.ganadero.animales.qr;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QrRateLimiter {
    private final QrProperties properties;
    private final ConcurrentHashMap<UUID, Window> windows = new ConcurrentHashMap<>();

    public QrRateLimiter(QrProperties properties) {
        this.properties = properties;
    }

    public void check(UUID userId) {
        int limit = properties.resolverRateLimitPerMinute();
        long now = System.currentTimeMillis();
        Window updated = windows.compute(userId, (key, current) -> {
            if (current == null || now - current.startMillis >= 60_000) {
                return new Window(now, 1);
            }
            return new Window(current.startMillis, current.count + 1);
        });
        if (updated.count > limit) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
    }

    private record Window(long startMillis, int count) {}
}
