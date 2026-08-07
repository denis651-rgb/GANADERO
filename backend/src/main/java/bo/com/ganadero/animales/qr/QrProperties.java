package bo.com.ganadero.animales.qr;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.qr")
public record QrProperties(
        @NotBlank String signingSecret,
        @Min(1) int payloadVersion,
        @Min(1) int maxPayloadBytes,
        @Min(1) int resolverRateLimitPerMinute) {

    public QrProperties {
        if (payloadVersion == 0) payloadVersion = 1;
        if (maxPayloadBytes == 0) maxPayloadBytes = 2048;
        if (resolverRateLimitPerMinute == 0) resolverRateLimitPerMinute = 20;
    }
}
