package bo.com.ganadero.reportes.api;

import bo.com.ganadero.reportes.application.ReporteService;
import bo.com.ganadero.reportes.domain.ReporteAnimalMuerto;
import bo.com.ganadero.reportes.domain.ReporteAnimalNacido;
import bo.com.ganadero.reportes.domain.ReporteVenta;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {
    private final ReporteService service;

    public ReporteController(ReporteService service) {
        this.service = service;
    }

    @GetMapping("/nacimientos")
    public ApiResponse<List<ReporteAnimalNacido>> nacimientos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {
        return success(service.nacimientos(desde, hasta), request);
    }

    @GetMapping("/muertes")
    public ApiResponse<List<ReporteAnimalMuerto>> muertes(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {
        return success(service.muertes(desde, hasta), request);
    }

    @GetMapping("/ventas")
    public ApiResponse<List<ReporteVenta>> ventas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {
        return success(service.ventas(desde, hasta), request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
