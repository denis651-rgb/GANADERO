package bo.com.ganadero.archivos.application;
import bo.com.ganadero.shared.config.AppProperties;import bo.com.ganadero.shared.error.*;import bo.com.ganadero.shared.security.*;
import org.springframework.stereotype.Service;import org.springframework.web.multipart.MultipartFile;import java.io.IOException;import java.util.*;
@Service public class StorageService{
 private final SupabaseStorageClient client;private final AppProperties properties;private final UserContext context;private final ImagenValidador validador;
 public StorageService(SupabaseStorageClient client,AppProperties properties,UserContext context,ImagenValidador validador){this.client=client;this.properties=properties;this.context=context;this.validador=validador;}
 public StoredAvatar uploadAvatar(MultipartFile file){CurrentUser user=context.currentUser();byte[] content=bytes(file);validate(file,content);String extension=extension(file.getContentType());
  String path="empresas/"+user.empresaId()+"/usuarios/"+user.userId()+"/avatar/"+UUID.randomUUID()+"."+extension;
  client.upload(path,content,file.getContentType());return new StoredAvatar(path,client.signedUrl(path));}
 public String signedAvatar(String path){CurrentUser user=context.currentUser();String prefix="empresas/"+user.empresaId()+"/usuarios/"+user.userId()+"/avatar/";
  if(path==null||!path.startsWith(prefix)||path.contains(".."))throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);return client.signedUrl(path);}
 private byte[] bytes(MultipartFile file){try{return file.getBytes();}catch(IOException exception){throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);}}
 private void validate(MultipartFile file,byte[] content){if(file==null||file.isEmpty()||file.getSize()>properties.storage().maxBytes()||!properties.storage().allowedMimeTypes().contains(file.getContentType()))throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
  String name=Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);if(name.chars().filter(c->c=='.').count()!=1||!properties.storage().allowedExtensions().contains(name.substring(name.lastIndexOf('.')+1)))throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
  validador.validar(content,file.getContentType());}
 private String extension(String mime){return switch(mime){case "image/jpeg"->"jpg";case "image/png"->"png";case "image/webp"->"webp";default->throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);};}
 public record StoredAvatar(String path,String signedUrl){}
}
