package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.application.ConsultarUsuarioActualUseCase;import bo.com.ganadero.shared.api.ApiResponse;import bo.com.ganadero.shared.security.UserContext;import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/auth") public class AuthController{
 private final ConsultarUsuarioActualUseCase current;private final UserContext context;public AuthController(ConsultarUsuarioActualUseCase current,UserContext context){this.current=current;this.context=context;}
 @GetMapping("/me")public ApiResponse<AuthMeResponse> me(HttpServletRequest r){return ApiResponse.success(AuthMeResponse.from(current.execute()),id(r));}
 @GetMapping("/permisos")public ApiResponse<Set<String>> permissions(HttpServletRequest r){return ApiResponse.success(context.currentUser().permisos(),id(r));}
 private String id(HttpServletRequest r){Object v=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);return v==null?"unknown":v.toString();}}
