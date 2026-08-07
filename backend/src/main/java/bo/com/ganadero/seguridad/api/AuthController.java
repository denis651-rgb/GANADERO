package bo.com.ganadero.seguridad.api;
import bo.com.ganadero.seguridad.application.ConsultarUsuarioActualUseCase;import bo.com.ganadero.seguridad.invitaciones.ActivacionInvitacionResponse;import bo.com.ganadero.seguridad.invitaciones.InvitacionService;import bo.com.ganadero.shared.api.ApiResponse;import bo.com.ganadero.shared.error.BusinessException;import bo.com.ganadero.shared.error.ErrorCode;import bo.com.ganadero.shared.security.UserContext;import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.security.oauth2.jwt.Jwt;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/v1/auth") public class AuthController{
 private final ConsultarUsuarioActualUseCase current;private final UserContext context;private final InvitacionService invitaciones;
 public AuthController(ConsultarUsuarioActualUseCase current,UserContext context,InvitacionService invitaciones){this.current=current;this.context=context;this.invitaciones=invitaciones;}
 @GetMapping("/me")public ApiResponse<AuthMeResponse> me(HttpServletRequest r){return ApiResponse.success(AuthMeResponse.from(current.execute()),id(r));}
 @GetMapping("/permisos")public ApiResponse<Set<String>> permissions(HttpServletRequest r){return ApiResponse.success(context.currentUser().permisos(),id(r));}
 @PostMapping("/activar-invitacion")public ApiResponse<ActivacionInvitacionResponse> activarInvitacion(@AuthenticationPrincipal Jwt jwt,HttpServletRequest r){
  if(jwt==null||jwt.getSubject()==null)throw new BusinessException(ErrorCode.UNAUTHENTICATED);
  return ApiResponse.success(invitaciones.aceptar(jwt.getSubject()),id(r));}
 private String id(HttpServletRequest r){Object v=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);return v==null?"unknown":v.toString();}}
