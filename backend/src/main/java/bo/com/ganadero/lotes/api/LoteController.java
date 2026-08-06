package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.application.LoteService;
import bo.com.ganadero.lotes.domain.EstadoLote;
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
@RequestMapping("/api/v1/lotes")
public class LoteController {
    private final LoteService service;

    public LoteController(LoteService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<LotePageResponse> list(@RequestParam(required = false) EstadoLote estado,
                                       @RequestParam(required = false) String search,
                                       @RequestParam(defaultValue = "0") @Min(0) int page,
                                       @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
                                       HttpServletRequest request) {
        return ok(LotePageResponse.from(service.list(estado, search, page, size)), request);
    }

    @PostMapping
    ApiResponse<LoteResponse> create(@Valid @RequestBody CrearLoteRequest body, HttpServletRequest request) {
        return ok(LoteResponse.from(service.create(body.command())), request);
    }

    @GetMapping("/{id}")
    ApiResponse<LoteResponse> get(@PathVariable UUID id, HttpServletRequest request) {
        return ok(LoteResponse.from(service.get(id)), request);
    }

    @PatchMapping("/{id}")
    ApiResponse<LoteResponse> update(@PathVariable UUID id, @Valid @RequestBody ActualizarLoteRequest body,
                                     HttpServletRequest request) {
        return ok(LoteResponse.from(service.update(id, body.command())), request);
    }

    @PostMapping("/{id}/cerrar")
    ApiResponse<LoteResponse> close(@PathVariable UUID id, @Valid @RequestBody CerrarLoteRequest body,
                                    HttpServletRequest request) {
        return ok(LoteResponse.from(service.close(id, body.version())), request);
    }

    @GetMapping("/{id}/animales")
    ApiResponse<List<MembresiaResponse>> animales(@PathVariable UUID id,
                                                  @RequestParam(defaultValue = "true") boolean activos,
                                                  HttpServletRequest request) {
        return ok(service.memberships(id, activos).stream().map(MembresiaResponse::from).toList(), request);
    }

    @PostMapping("/{id}/animales")
    ApiResponse<List<MembresiaResponse>> addAnimales(@PathVariable UUID id,
                                                     @Valid @RequestBody LoteAnimalesRequest body,
                                                     HttpServletRequest request) {
        return ok(service.addAnimals(id, body.animalIds()).stream().map(MembresiaResponse::from).toList(), request);
    }

    @PostMapping("/{id}/retirar-animales")
    ApiResponse<List<MembresiaResponse>> removeAnimales(@PathVariable UUID id,
                                                        @Valid @RequestBody LoteAnimalesRequest body,
                                                        HttpServletRequest request) {
        return ok(service.removeAnimals(id, body.animalIds(), body.motivo()).stream().map(MembresiaResponse::from).toList(), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
