package bo.com.ganadero.shared.status;

import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.Map;

@RestController @RequestMapping("/api/v1/system")
public class SystemStatusController {
    private final AppProperties properties; private final UserContext context;
    public SystemStatusController(AppProperties properties,UserContext context){this.properties=properties;this.context=context;}
    @GetMapping("/status") public ApiResponse<Map<String,Object>> status(HttpServletRequest request){
        if(!properties.systemStatus().enabled())throw new BusinessException(ErrorCode.SYSTEM_STATUS_DISABLED);
        context.requirePermission("SISTEMA_ESTADO_VER");
        return ApiResponse.success(Map.of("status","UP","version","0.0.1","environment","configured","date",Instant.now()),correlationId(request));
    }
    private String correlationId(HttpServletRequest request){Object value=request.getAttribute(CorrelationIdFilter.ATTRIBUTE);return value==null?"unknown":value.toString();}
}
