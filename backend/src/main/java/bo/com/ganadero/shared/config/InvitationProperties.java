package bo.com.ganadero.shared.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.invitations")
public record InvitationProperties(
        @Min(1) int expirationHours,
        @Min(1) @Max(50) int maxResendAttempts) {
}
