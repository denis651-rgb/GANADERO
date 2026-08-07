package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.application.HistorialLoteFilter;
import bo.com.ganadero.lotes.application.IngresoMasivoResultado;
import bo.com.ganadero.lotes.application.LoteService;
import bo.com.ganadero.lotes.application.RetiroMasivoResultado;
import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
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
        return ok(LoteResponse.from(service.close(id, body.version(), body.fechaCierre(), body.motivo())), request);
    }

    @GetMapping("/{id}/animales")
    ApiResponse<List<MembresiaResponse>> animales(@PathVariable UUID id,
                                                  @RequestParam(defaultValue = "true") boolean activos,
                                                  HttpServletRequest request) {
        return ok(service.memberships(id, activos).stream().map(MembresiaResponse::from).toList(), request);
    }

    @PostMapping("/{id}/animales")
    ApiResponse<IngresoMasivoResultado> addAnimales(@PathVariable UUID id,
                                                    @Valid @RequestBody IngresoLoteRequest body,
                                                    HttpServletRequest request) {
        return ok(service.addAnimals(id, body.command()), request);
    }

    @PostMapping("/{id}/retirar-animales")
    ApiResponse<RetiroMasivoResultado> removeAnimales(@PathVariable UUID id,
                                                      @Valid @RequestBody RetiroLoteRequest body,
                                                      HttpServletRequest request) {
        return ok(service.removeAnimals(id, body.command()), request);
    }

    @GetMapping("/{id}/historial")
    ApiResponse<MembresiaLotePageResponse> historial(@PathVariable UUID id,
                                                     @RequestParam(required = false) UUID animalId,
                                                     @RequestParam(required = false) Instant desde,
                                                     @RequestParam(required = false) Instant hasta,
                                                     @RequestParam(required = false) String motivoIngreso,
                                                     @RequestParam(required = false) String motivoSalida,
                                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                                     @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
                                                     HttpServletRequest request) {
        HistorialLoteFilter filter = new HistorialLoteFilter(animalId, desde, hasta,
                motivoIngreso, motivoSalida, page, size);
        return ok(MembresiaLotePageResponse.from(service.historial(id, filter)), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
