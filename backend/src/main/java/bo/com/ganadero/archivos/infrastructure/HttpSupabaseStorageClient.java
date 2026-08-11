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

import java.util.Map;

@Component
public class HttpSupabaseStorageClient implements SupabaseStorageClient {
    private static final Logger LOG = LoggerFactory.getLogger(HttpSupabaseStorageClient.class);
    private final RestClient client;
    private final String key;
    private final AppProperties properties;

    public HttpSupabaseStorageClient(@Value("${SUPABASE_URL:}") String url,
                                     @Value("${SUPABASE_SERVICE_ROLE_KEY:}") String key,
                                     RestClient.Builder builder,
                                     AppProperties properties) {
        this.key = key;
        this.properties = properties;
        this.client = url == null || url.isBlank() ? builder.build() : builder.baseUrl(url).build();
    }

    @Override
    public void upload(String path, byte[] content, String contentType) {
        configured();
        try {
            client.post().uri("/storage/v1/object/{bucket}/{path}", properties.storage().bucket(), path)
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
            Map<String, Object> response = client.post().uri("/storage/v1/object/sign/{bucket}/{path}",
                            properties.storage().bucket(), path)
                    .header("apikey", key)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("expiresIn", properties.storage().signedUrlTtl().toSeconds()))
                    .retrieve().body(Map.class);
            Object signed = response == null ? null : response.get("signedURL");
            if (signed == null) {
                throw storageFailure("signedUrl", path, null);
            }
            return String.valueOf(signed);
        } catch (RestClientException exception) {
            throw storageFailure("signedUrl", path, exception);
        }
    }

    @Override
    public void delete(String path) {
        configured();
        try {
            client.delete().uri("/storage/v1/object/{bucket}/{path}", properties.storage().bucket(), path)
                    .header("apikey", key)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                    .retrieve().toBodilessEntity();
        } catch (RestClientException exception) {
            throw storageFailure("delete", path, exception);
        }
    }

    private void configured() {
        if (key == null || key.isBlank()) {
            LOG.error("Supabase Storage no configurado: falta SUPABASE_SERVICE_ROLE_KEY.");
            throw new BusinessException(ErrorCode.STORAGE_NOT_CONFIGURED);
        }
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
