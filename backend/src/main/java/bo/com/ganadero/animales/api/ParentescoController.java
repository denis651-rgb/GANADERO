package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.ParentescoService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animales/{animalId}/parentescos")
public class ParentescoController {
    private final ParentescoService service;

    public ParentescoController(ParentescoService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<ParentescoResponse>> list(@PathVariable UUID animalId, HttpServletRequest request) {
        return ok(service.list(animalId).stream().map(ParentescoResponse::from).toList(), request);
    }

    @PostMapping
    ApiResponse<ParentescoResponse> create(@PathVariable UUID animalId,
                                           @Valid @RequestBody CrearParentescoRequest body,
                                           HttpServletRequest request) {
        return ok(ParentescoResponse.from(service.create(animalId, body.command())), request);
    }

    @PatchMapping("/{parentescoId}")
    ApiResponse<ParentescoResponse> update(@PathVariable UUID animalId,
                                           @PathVariable UUID parentescoId,
                                           @Valid @RequestBody ActualizarParentescoRequest body,
                                           HttpServletRequest request) {
        return ok(ParentescoResponse.from(service.update(animalId, parentescoId, body.command())), request);
    }

    @DeleteMapping("/{parentescoId}")
    ApiResponse<Void> delete(@PathVariable UUID animalId, @PathVariable UUID parentescoId, HttpServletRequest request) {
        service.delete(animalId, parentescoId);
        return ok(null, request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
