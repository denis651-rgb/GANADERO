package bo.com.ganadero.archivos.infrastructure;
import bo.com.ganadero.archivos.application.SupabaseStorageClient;import bo.com.ganadero.shared.config.AppProperties;import bo.com.ganadero.shared.error.*;
import org.springframework.beans.factory.annotation.Value;import org.springframework.http.*;import org.springframework.stereotype.Component;import org.springframework.web.client.*;import java.util.Map;
@Component public class HttpSupabaseStorageClient implements SupabaseStorageClient{
 private final RestClient client;private final String key;private final AppProperties properties;
 public HttpSupabaseStorageClient(@Value("${SUPABASE_URL:}")String url,@Value("${SUPABASE_SERVICE_ROLE_KEY:}")String key,RestClient.Builder builder,AppProperties properties){this.key=key;this.properties=properties;this.client=url==null||url.isBlank()?builder.build():builder.baseUrl(url).build();}
 public void upload(String path,byte[] content,String contentType){configured();try{client.post().uri("/storage/v1/object/{bucket}/{path}",properties.storage().bucket(),path)
  .header("apikey",key).header(HttpHeaders.AUTHORIZATION,"Bearer "+key).header("x-upsert","true").contentType(MediaType.parseMediaType(contentType)).body(content).retrieve().toBodilessEntity();}catch(RestClientException e){throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);}}
 public String signedUrl(String path){configured();try{@SuppressWarnings("unchecked")Map<String,Object> response=client.post().uri("/storage/v1/object/sign/{bucket}/{path}",properties.storage().bucket(),path)
  .header("apikey",key).header(HttpHeaders.AUTHORIZATION,"Bearer "+key).contentType(MediaType.APPLICATION_JSON).body(Map.of("expiresIn",properties.storage().signedUrlTtl().toSeconds())).retrieve().body(Map.class);
  Object signed=response==null?null:response.get("signedURL");if(signed==null)throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);return String.valueOf(signed);}catch(RestClientException e){throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);}}
 public void delete(String path){configured();try{client.delete().uri("/storage/v1/object/{bucket}/{path}",properties.storage().bucket(),path).header("apikey",key).header(HttpHeaders.AUTHORIZATION,"Bearer "+key).retrieve().toBodilessEntity();}catch(RestClientException e){throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);}}
 private void configured(){if(key==null||key.isBlank())throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);}
}
