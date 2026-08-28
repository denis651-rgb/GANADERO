package bo.com.ganadero.archivos.api;
import bo.com.ganadero.archivos.application.StorageService;import bo.com.ganadero.shared.api.ApiResponse;import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/v1/perfil/avatar") public class AvatarController{
 private final StorageService storage;public AvatarController(StorageService storage){this.storage=storage;}
 @PostMapping(consumes="multipart/form-data")public ApiResponse<StorageService.StoredAvatar> upload(@RequestPart("file")MultipartFile file,HttpServletRequest request){var avatar=storage.uploadAvatar(file);Object c=request.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(avatar,c==null?"unknown":c.toString());}
}
