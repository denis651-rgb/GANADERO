package bo.com.ganadero.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Bootstrap bootstrap,
        InternalJobs internalJobs,
        SystemStatus systemStatus,
        String frontendUrl,
        Storage storage) {

    public record Bootstrap(boolean enabled, String token) {}
    public record InternalJobs(boolean enabled, String secret) {}
    public record SystemStatus(boolean enabled) {}
    public record Storage(String bucket, long maxBytes, Duration signedUrlTtl,
                          List<String> allowedMimeTypes, List<String> allowedExtensions) {}
}
