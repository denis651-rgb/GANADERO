package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.CategoriaAnimalConfigService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/configuracion/categorias-edad")
public class CategoriaAnimalConfigController {
    private final CategoriaAnimalConfigService service;

    public CategoriaAnimalConfigController(CategoriaAnimalConfigService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<CategoriaAnimalResponse>> listar(HttpServletRequest r) {
        return ok(service.listarTodas().stream().map(CategoriaAnimalResponse::from).toList(), r);
    }

    @PostMapping
    ApiResponse<CategoriaAnimalResponse> crear(@Valid @RequestBody RangoCategoriaRequest b, HttpServletRequest r) {
        return ok(CategoriaAnimalResponse.from(service.crear(b.command())), r);
    }

    @PutMapping("/{id}")
    ApiResponse<CategoriaAnimalResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody RangoCategoriaRequest b,
                                                    HttpServletRequest r) {
        return ok(CategoriaAnimalResponse.from(service.actualizar(id, b.command())), r);
    }

    @PatchMapping("/{id}/estado")
    ApiResponse<Void> cambiarEstado(@PathVariable UUID id, @Valid @RequestBody CambiarEstadoCategoriaRequest b,
                                    HttpServletRequest r) {
        service.cambiarEstado(id, b.activo());
        return ok(null, r);
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> eliminar(@PathVariable UUID id, HttpServletRequest r) {
        service.eliminar(id);
        return ok(null, r);
    }

    @PostMapping("/simular")
    ApiResponse<SimulacionCategoriaResponse> simular(@Valid @RequestBody RangoCategoriaRequest b, HttpServletRequest r) {
        return ok(new SimulacionCategoriaResponse(service.simular(b.command(), b.id())), r);
    }

    @PostMapping("/reclasificar")
    ApiResponse<ResultadoReclasificacionResponse> reclasificar(HttpServletRequest r) {
        return ok(ResultadoReclasificacionResponse.from(service.reclasificarManual()), r);
    }

    private <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        Object c = r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(d, c == null ? "unknown" : c.toString());
    }
}
