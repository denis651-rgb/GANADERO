package bo.com.ganadero.archivos.application;
import bo.com.ganadero.shared.config.AppProperties;import bo.com.ganadero.shared.error.*;import bo.com.ganadero.shared.security.*;
import org.junit.jupiter.api.Test;import org.springframework.mock.web.MockMultipartFile;import java.time.Duration;import java.util.*;import static org.assertj.core.api.Assertions.*;
class StorageServiceTest{
 @Test void rejectsSvg(){StorageService service=service();var file=new MockMultipartFile("file","avatar.svg","image/svg+xml","<svg/>".getBytes());assertThatThrownBy(()->service.uploadAvatar(file)).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.code()).isEqualTo(ErrorCode.STORAGE_FILE_INVALID));}
 @Test void buildsTenantOwnedUuidPath(){CapturingClient client=new CapturingClient();CurrentUser user=user();StorageService service=service(client,user);var file=new MockMultipartFile("file","avatar.png","image/png",new byte[]{1,2,3});var result=service.uploadAvatar(file);assertThat(result.path()).startsWith("empresas/"+user.empresaId()+"/usuarios/"+user.userId()+"/avatar/").endsWith(".png");}
 private StorageService service(){return service(new CapturingClient(),user());}
 private StorageService service(SupabaseStorageClient client,CurrentUser user){AppProperties p=new AppProperties(new AppProperties.Bootstrap(false,""),new AppProperties.SystemStatus(false),"http://localhost",new AppProperties.Storage("bucket",1024,Duration.ofMinutes(5),List.of("image/png"),List.of("png")));return new StorageService(client,p,new UserContext(()->user));}
 private CurrentUser user(){return new CurrentUser(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),Set.of(),Set.of(),Set.of(),true);}
 static class CapturingClient implements SupabaseStorageClient{public void upload(String path,byte[] content,String type){}public String signedUrl(String path){return "signed";}public void delete(String path){}}
}
