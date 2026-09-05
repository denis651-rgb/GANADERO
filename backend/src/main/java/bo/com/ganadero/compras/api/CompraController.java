package bo.com.ganadero.compras.api;

import bo.com.ganadero.compras.application.CompraService;
import bo.com.ganadero.compras.domain.EstadoCompra;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/compras")
public class CompraController {
    private final CompraService service;

    public CompraController(CompraService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<CompraPageResponse> list(@RequestParam(required = false) EstadoCompra estado,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
                                         HttpServletRequest r) {
        return ok(CompraPageResponse.from(service.list(estado, page, size)), r);
    }

    @GetMapping("/{id}")
    ApiResponse<CompraResponse> get(@PathVariable UUID id, HttpServletRequest r) {
        return ok(CompraResponse.from(service.get(id)), r);
    }

    @GetMapping("/{id}/detalles")
    ApiResponse<List<CompraDetalleResponse>> detalles(@PathVariable UUID id, HttpServletRequest r) {
        return ok(service.detalles(id).stream().map(CompraDetalleResponse::from).toList(), r);
    }

    @GetMapping("/{id}/dependencias")
    ApiResponse<List<DependenciaCompraResponse>> dependencias(@PathVariable UUID id, HttpServletRequest r) {
        return ok(service.verificarDependencias(id).stream().map(DependenciaCompraResponse::from).toList(), r);
    }

    @GetMapping("/animal/{animalId}")
    ApiResponse<ResumenCompraAnimalResponse> resumenParaAnimal(@PathVariable UUID animalId, HttpServletRequest r) {
        return ok(service.resumenParaAnimal(animalId).map(ResumenCompraAnimalResponse::from).orElse(null), r);
    }

    @PostMapping
    ApiResponse<CompraResponse> crear(@Valid @RequestBody CompraRequest b, HttpServletRequest r) {
        return ok(CompraResponse.from(service.crearBorrador(b.command())), r);
    }

    @PutMapping("/{id}")
    ApiResponse<CompraResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody CompraRequest b, HttpServletRequest r) {
        return ok(CompraResponse.from(service.actualizarBorrador(id, b.command())), r);
    }

    @PostMapping("/{id}/confirmar")
    ApiResponse<CompraResponse> confirmar(@PathVariable UUID id, @Valid @RequestBody ConfirmarCompraRequest b,
                                          HttpServletRequest r) {
        return ok(CompraResponse.from(service.confirmar(id, b.version())), r);
    }

    @PostMapping("/{id}/anular")
    ApiResponse<CompraResponse> anular(@PathVariable UUID id, @Valid @RequestBody AnularCompraRequest b,
                                       HttpServletRequest r) {
        return ok(CompraResponse.from(service.anular(id, b.motivo(), b.version())), r);
    }

    private <T> ApiResponse<T> ok(T d, HttpServletRequest r) {
        Object c = r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(d, c == null ? "unknown" : c.toString());
    }
}
