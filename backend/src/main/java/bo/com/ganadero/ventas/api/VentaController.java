package bo.com.ganadero.ventas.api;

import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import bo.com.ganadero.ventas.application.VentaService;
import bo.com.ganadero.ventas.domain.Venta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ventas")
public class VentaController {
    private final VentaService service;

    public VentaController(VentaService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Venta> registrar(@Valid @RequestBody CrearVentaRequest body, HttpServletRequest request) {
        return success(service.registrar(body.toCommand()), request);
    }

    @GetMapping
    public ApiResponse<List<Venta>> list(@RequestParam(required = false) UUID animalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            HttpServletRequest request) {
        return success(service.list(animalId, desde, hasta), request);
    }

    @GetMapping("/{id}")
    public ApiResponse<Venta> get(@PathVariable UUID id, HttpServletRequest request) {
        return success(service.get(id), request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
