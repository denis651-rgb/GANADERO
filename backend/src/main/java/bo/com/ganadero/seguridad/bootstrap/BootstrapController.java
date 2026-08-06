package bo.com.ganadero.seguridad.bootstrap;
import bo.com.ganadero.shared.api.ApiResponse;import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;import jakarta.validation.Valid;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/bootstrap") public class BootstrapController{
 private final BootstrapService service;public BootstrapController(BootstrapService service){this.service=service;}
 @PostMapping("/empresa-inicial") public ApiResponse<BootstrapResponse> create(
  @RequestHeader(value="X-Bootstrap-Token",required=false)String token,@RequestHeader(value="Idempotency-Key",required=false)String key,
  @RequestBody BootstrapRequest body,HttpServletRequest request){Object value=request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
  String correlation=value==null?"unknown":value.toString();return ApiResponse.success(service.execute(token,key,body,correlation),correlation);}}
