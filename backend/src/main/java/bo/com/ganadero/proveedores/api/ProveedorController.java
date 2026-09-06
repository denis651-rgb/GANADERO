package bo.com.ganadero.proveedores.api;

import bo.com.ganadero.proveedores.application.ProveedorService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/proveedores")
public class ProveedorController {
    private final ProveedorService service;

    public ProveedorController(ProveedorService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<ProveedorResponse>> buscar(@RequestParam(required = false) String q,
                                                @RequestParam(defaultValue = "true") boolean soloActivos,
                                                HttpServletRequest r) {
        return ok(service.buscar(q, soloActivos).stream().map(ProveedorResponse::from).toList(), r);
    }

    @GetMapping("/{id}")
    ApiResponse<ProveedorResponse> get(@PathVariable UUID id, HttpServletRequest r) {
        return ok(ProveedorResponse.from(service.get(id)), r);
    }

    @PostMapping
    ApiResponse<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest b, HttpServletRequest r) {
        return ok(ProveedorResponse.from(service.crear(b.command())), r);
    }

    @PutMapping("/{id}")
    ApiResponse<ProveedorResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody ProveedorRequest b,
                                              HttpServletRequest r) {
        return ok(ProveedorResponse.from(service.actualizar(id, b.command())), r);
    }

    @PatchMapping("/{id}/estado")
    ApiResponse<ProveedorResponse> cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoProveedorRequest b,
                                                 HttpServletRequest r) {
        return ok(ProveedorResponse.from(service.cambiarEstado(id, b.activo(), b.version())), r);
    }

    private <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        Object c = r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(d, c == null ? "unknown" : c.toString());
    }
}
