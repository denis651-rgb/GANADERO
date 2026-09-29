package bo.com.ganadero.potreros.api;

import bo.com.ganadero.potreros.application.PotreroService;
import bo.com.ganadero.potreros.domain.EstadoPotrero;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1")
public class PotreroController {

    private final PotreroService service;

    public PotreroController(PotreroService service) {
        this.service = service;
    }

    @GetMapping("/tipos-pasto")
    public ApiResponse<List<TipoPastoResponse>> grasses(HttpServletRequest request) {
        return ok(service.grasses().stream().map(TipoPastoResponse::from).toList(), request);
    }

    @GetMapping("/potreros")
    public ApiResponse<PotreroPageResponse> list(
            @RequestParam(required = false) UUID propiedadId,
            @RequestParam(required = false) EstadoPotrero estado,
            @RequestParam(required = false) UUID sectorId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
            HttpServletRequest request) {
        return ok(PotreroPageResponse.from(service.listPage(propiedadId, estado, sectorId, page, size)), request);
    }

    @PostMapping("/potreros")
    public ApiResponse<PotreroResponse> create(@Valid @RequestBody CrearPotreroRequest body, HttpServletRequest request) {
        return ok(PotreroResponse.from(service.create(body.command())), request);
    }

    @GetMapping("/potreros/{id}")
    public ApiResponse<PotreroResponse> get(@PathVariable UUID id, HttpServletRequest request) {
        return ok(PotreroResponse.from(service.get(id)), request);
    }

    @PatchMapping("/potreros/{id}")
    public ApiResponse<PotreroResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarPotreroRequest body,
            HttpServletRequest request) {
        return ok(PotreroResponse.from(service.update(id, body.command())), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlationId == null ? "unknown" : correlationId.toString());
    }
}

