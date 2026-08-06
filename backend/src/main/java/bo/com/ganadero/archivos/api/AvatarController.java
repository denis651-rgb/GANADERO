package bo.com.ganadero.archivos.api;
import bo.com.ganadero.archivos.application.StorageService;import bo.com.ganadero.shared.api.ApiResponse;import bo.com.ganadero.shared.security.*;import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;import org.springframework.jdbc.core.simple.JdbcClient;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/perfil/avatar") public class AvatarController{
 private final StorageService storage;private final JdbcClient jdbc;private final UserContext context;public AvatarController(StorageService storage,JdbcClient jdbc,UserContext context){this.storage=storage;this.jdbc=jdbc;this.context=context;}
 @PostMapping(consumes="multipart/form-data")public ApiResponse<StorageService.StoredAvatar> upload(@RequestPart("file")MultipartFile file,HttpServletRequest request){var avatar=storage.uploadAvatar(file);CurrentUser user=context.currentUser();
  jdbc.sql("update seguridad.perfiles_usuario set avatar_path=:path,updated_at=now(),version=version+1 where id=:id").param("path",avatar.path()).param("id",user.userId()).update();Object c=request.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(avatar,c==null?"unknown":c.toString());}
}
