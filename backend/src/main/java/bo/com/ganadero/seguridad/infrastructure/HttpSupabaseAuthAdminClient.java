package bo.com.ganadero.seguridad.infrastructure;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
class HttpSupabaseAuthAdminClient implements SupabaseAuthAdminClient {
    private final RestClient client;
    private final String serviceKey;

    HttpSupabaseAuthAdminClient(@Value("${SUPABASE_URL:}") String url,
                                @Value("${SUPABASE_SERVICE_ROLE_KEY:}") String serviceKey,
                                RestClient.Builder builder) {
        this.serviceKey = serviceKey;
        this.client = url == null || url.isBlank() ? builder.build() : builder.baseUrl(url).build();
    }

    @Override
    public AdminUser invite(String email, String redirectTo) {
        requireConfigured();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post().uri("/auth/v1/invite")
                    .header("apikey", serviceKey).header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("email", email, "redirect_to", redirectTo))
                    .retrieve().body(Map.class);
            return new AdminUser(UUID.fromString(String.valueOf(response.get("id"))), true);
        } catch (HttpClientErrorException.UnprocessableEntity duplicate) {
            return findByEmail(email);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SUPABASE_AUTH_UNAVAILABLE);
        }
    }

    @Override
    public void deleteIfCreated(AdminUser user) {
        if (!user.createdByOperation()) return;
        try {
            client.delete().uri("/auth/v1/admin/users/{id}", user.id())
                    .header("apikey", serviceKey).header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException ignored) {
            // Compensación de mejor esfuerzo; nunca se expone la respuesta administrativa.
        }
    }

    private AdminUser findByEmail(String email) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.get()
                    .uri(uri -> uri.path("/auth/v1/admin/users").queryParam("filter", email).build())
                    .header("apikey", serviceKey).header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                    .retrieve().body(Map.class);
            Object raw = response == null ? null : response.get("users");
            if (raw instanceof List<?> users) for (Object candidate : users) {
                if (candidate instanceof Map<?, ?> user && email.equalsIgnoreCase(String.valueOf(user.get("email"))))
                    return new AdminUser(UUID.fromString(String.valueOf(user.get("id"))), false);
            }
            throw new BusinessException(ErrorCode.SUPABASE_AUTH_UNAVAILABLE);
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.SUPABASE_AUTH_UNAVAILABLE);
        }
    }

    private void requireConfigured() {
        if (serviceKey == null || serviceKey.isBlank()) throw new BusinessException(ErrorCode.SUPABASE_AUTH_UNAVAILABLE);
    }
}
