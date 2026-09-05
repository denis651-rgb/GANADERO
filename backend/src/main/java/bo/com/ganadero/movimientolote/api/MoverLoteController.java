package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.application.MoverLoteService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
public class MoverLoteController {
    private final MoverLoteService service;

    public MoverLoteController(MoverLoteService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/lotes/{loteId}/movimiento-lote/preparar")
    ApiResponse<PreparacionMovimientoLoteResponse> preparar(@PathVariable UUID loteId,
                                                            @Valid @RequestBody PrepararMovimientoLoteRequest body,
                                                            HttpServletRequest r) {
        return ok(PreparacionMovimientoLoteResponse.from(service.preparar(loteId, body.command())), r);
    }

    @GetMapping("/api/v1/movimiento-lote/preparaciones/{id}")
    ApiResponse<PreparacionMovimientoLoteResponse> obtener(@PathVariable UUID id, HttpServletRequest r) {
        return ok(PreparacionMovimientoLoteResponse.from(service.obtenerPreparacion(id)), r);
    }

    @PostMapping("/api/v1/movimiento-lote/preparaciones/{id}/confirmar")
    ApiResponse<ResultadoMovimientoLoteResponse> confirmar(@PathVariable UUID id,
                                                           @Valid @RequestBody ConfirmarMovimientoLoteRequest body,
                                                           HttpServletRequest r) {
        return ok(ResultadoMovimientoLoteResponse.from(service.confirmar(id, body.command())), r);
    }

    @PostMapping("/api/v1/movimiento-lote/preparaciones/{id}/cancelar")
    ApiResponse<Void> cancelar(@PathVariable UUID id, HttpServletRequest r) {
        service.cancelar(id);
        return ok(null, r);
    }

    @PostMapping("/api/v1/movimiento-lote/preparaciones/{id}/anular")
    ApiResponse<AnulacionMovimientoLoteResponse> anular(@PathVariable UUID id,
                                                        @Valid @RequestBody AnularMovimientoLoteRequest body,
                                                        HttpServletRequest r) {
        return ok(AnulacionMovimientoLoteResponse.from(service.anular(id, body.motivo())), r);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest r) {
        Object c = r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, c == null ? "unknown" : c.toString());
    }
}
