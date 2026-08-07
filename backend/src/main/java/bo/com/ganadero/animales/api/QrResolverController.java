package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.QrService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/qr")
public class QrResolverController {
    private final QrService service;

    public QrResolverController(QrService service) {
        this.service = service;
    }

    @PostMapping("/resolver")
    ApiResponse<QrService.QrResolveResult> resolver(@Valid @RequestBody ResolverQrRequest body,
                                                    HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(service.resolve(body.payload()), value == null ? "unknown" : value.toString());
    }
}
