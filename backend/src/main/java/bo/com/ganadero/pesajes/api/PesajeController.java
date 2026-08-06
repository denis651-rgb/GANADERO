package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.application.PesajeService;
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
@RequestMapping("/api/v1/pesajes")
public class PesajeController {
    private final PesajeService service;

    public PesajeController(PesajeService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<PesajePageResponse> list(@RequestParam(required = false) UUID animalId,
                                         @RequestParam(required = false) UUID propiedadId,
                                         @RequestParam(defaultValue = "0") @Min(0) int page,
                                         @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
                                         HttpServletRequest request) {
        return ok(PesajePageResponse.from(service.list(animalId, propiedadId, page, size)), request);
    }

    @GetMapping("/animal/{animalId}")
    ApiResponse<List<PesajeResponse>> history(@PathVariable UUID animalId, HttpServletRequest request) {
        return ok(service.history(animalId).stream().map(PesajeResponse::from).toList(), request);
    }

    @PostMapping
    ApiResponse<PesajeResponse> registrar(@Valid @RequestBody RegistrarPesajeRequest body, HttpServletRequest request) {
        return ok(PesajeResponse.from(service.registrar(body.command())), request);
    }

    @PostMapping("/lote")
    ApiResponse<List<PesajeResponse>> registrarLote(@Valid @RequestBody PesajeLoteRequest body, HttpServletRequest request) {
        return ok(service.registrarLote(body.command()).stream().map(PesajeResponse::from).toList(), request);
    }

    @GetMapping("/{id}")
    ApiResponse<PesajeResponse> get(@PathVariable UUID id, HttpServletRequest request) {
        return ok(PesajeResponse.from(service.get(id)), request);
    }

    @PostMapping("/{id}/anular")
    ApiResponse<PesajeResponse> anular(@PathVariable UUID id, @Valid @RequestBody AnularPesajeRequest body,
                                       HttpServletRequest request) {
        return ok(PesajeResponse.from(service.anular(id, body.motivo(), body.version())), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
