package bo.com.ganadero.archivos.infrastructure;

import bo.com.ganadero.archivos.application.SupabaseStorageClient;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Component
public class HttpSupabaseStorageClient implements SupabaseStorageClient {
    private static final Logger LOG = LoggerFactory.getLogger(HttpSupabaseStorageClient.class);
    private final RestClient client;
    private final String baseUrl;
    private final String key;
    private final AppProperties properties;

    public HttpSupabaseStorageClient(@Value("${SUPABASE_URL:}") String url,
                                     @Value("${SUPABASE_SERVICE_ROLE_KEY:}") String key,
                                     RestClient.Builder builder,
                                     AppProperties properties) {
        this.baseUrl = normalizeBaseUrl(url);
        this.key = key;
        this.properties = properties;
        this.client = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
    }

    @Override
    public void upload(String path, byte[] content, String contentType) {
        configured();
        try {
            client.post().uri(storageObjectUri(null, path))
                    .header("apikey", key)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .header("x-upsert", "true")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(content)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw storageFailure("upload", path, exception);
        }
    }

    @Override
    public String signedUrl(String path) {
        configured();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = client.post().uri(storageObjectUri("sign", path))
                    .header("apikey", key)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", properties.storage().signedUrlTtl().toSeconds()))
                    .retrieve().body(Map.class);
            Object signed = response == null ? null : response.get("signedURL");
            if (signed == null || String.valueOf(signed).isBlank()) {
                throw storageFailure("signedUrl", path, null);
            }
            return absoluteSignedUrl(String.valueOf(signed));
        } catch (RestClientException exception) {
            throw storageFailure("signedUrl", path, exception);
        }
    }

    @Override
    public void delete(String path) {
        configured();
        try {
            client.delete().uri(storageObjectUri(null, path))
                    .header("apikey", key)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw storageFailure("delete", path, exception);
        }
    }

    private void configured() {
        if (baseUrl.isBlank() || key == null || key.isBlank()) {
            LOG.error("Supabase Storage no configurado: falta SUPABASE_URL o SUPABASE_SERVICE_ROLE_KEY.");
            throw new BusinessException(ErrorCode.STORAGE_NOT_CONFIGURED);
        }
    }

    private String absoluteSignedUrl(String signedUrl) {
        String value = signedUrl.trim();
        if (value.startsWith("https://") || value.startsWith("http://")) {
            return value;
        }
        if (value.startsWith("/storage/v1/")) {
            return baseUrl + value;
        }
        if (value.startsWith("storage/v1/")) {
            return baseUrl + "/" + value;
        }
        if (value.startsWith("/")) {
            return baseUrl + "/storage/v1" + value;
        }
        return baseUrl + "/storage/v1/" + value;
    }

    private URI storageObjectUri(String operation, String path) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/storage/v1/object");
        if (operation != null && !operation.isBlank()) {
            builder.pathSegment(operation);
        }
        builder.pathSegment(properties.storage().bucket());
        for (String segment : path.split("/")) {
            if (!segment.isBlank()) {
                builder.pathSegment(segment);
            }
        }
        return builder.build().encode().toUri();
    }

    private static String normalizeBaseUrl(String url) {
        if (url == null) return "";
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private BusinessException storageFailure(String operation, String path, RestClientException cause) {
        if (cause != null) {
            LOG.error("Supabase Storage falló en {} (bucket={}, path={})",
                    operation, properties.storage().bucket(), path, cause);
        } else {
            LOG.error("Supabase Storage respondió sin URL firmada en {} (bucket={}, path={})",
                    operation, properties.storage().bucket(), path);
        }
        return new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
    }
}
