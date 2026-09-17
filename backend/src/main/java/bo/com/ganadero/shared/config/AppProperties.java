package bo.com.ganadero.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Bootstrap bootstrap,
        InternalJobs internalJobs,
        SystemStatus systemStatus,
        String frontendUrl,
        Storage storage,
        Cors cors) {

    public record Bootstrap(boolean enabled, String token) {}
    public record InternalJobs(boolean enabled, String secret) {}
    public record SystemStatus(boolean enabled) {}
    public record Storage(String rootPath, long maxBytes,
                          List<String> allowedMimeTypes, List<String> allowedExtensions) {}
    public record Cors(List<String> allowedOrigins) {
        public Cors { allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins; }
    }
}
