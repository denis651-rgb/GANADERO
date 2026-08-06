package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.IdentificadorService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animales/{animalId}/identificadores")
public class IdentificadorController {
    private final IdentificadorService service;

    public IdentificadorController(IdentificadorService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<IdentificadorResponse>> list(@PathVariable UUID animalId, HttpServletRequest request) {
        return ok(service.list(animalId).stream().map(IdentificadorResponse::from).toList(), request);
    }

    @PostMapping
    ApiResponse<IdentificadorResponse> assign(@PathVariable UUID animalId,
                                              @Valid @RequestBody AsignarIdentificadorRequest body,
                                              HttpServletRequest request) {
        return ok(IdentificadorResponse.from(service.assign(animalId, body.command())), request);
    }

    @PatchMapping("/{identificadorId}")
    ApiResponse<IdentificadorResponse> update(@PathVariable UUID animalId,
                                              @PathVariable UUID identificadorId,
                                              @Valid @RequestBody ActualizarIdentificadorRequest body,
                                              HttpServletRequest request) {
        return ok(IdentificadorResponse.from(service.update(animalId, identificadorId, body.command())), request);
    }

    @PostMapping("/{identificadorId}/retirar")
    ApiResponse<IdentificadorResponse> retire(@PathVariable UUID animalId,
                                              @PathVariable UUID identificadorId,
                                              @Valid @RequestBody RetirarIdentificadorRequest body,
                                              HttpServletRequest request) {
        return ok(IdentificadorResponse.from(service.retire(animalId, identificadorId, body.motivo(), body.version())), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
