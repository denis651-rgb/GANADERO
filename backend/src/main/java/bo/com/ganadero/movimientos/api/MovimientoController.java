package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.EstadoMovimiento;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
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
@RequestMapping("/api/v1/movimientos")
public class MovimientoController {
    private final MovimientoService service;

    public MovimientoController(MovimientoService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<MovimientoPageResponse> list(@RequestParam(required = false) EstadoMovimiento estado,
                                             @RequestParam(required = false) TipoMovimiento tipo,
                                             @RequestParam(defaultValue = "0") @Min(0) int page,
                                             @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
                                             HttpServletRequest request) {
        return ok(MovimientoPageResponse.from(service.list(estado, tipo, page, size)), request);
    }

    @PostMapping
    ApiResponse<MovimientoResponse> create(@Valid @RequestBody CrearMovimientoRequest body, HttpServletRequest request) {
        return ok(MovimientoResponse.from(service.create(body.command())), request);
    }

    @GetMapping("/{id}")
    ApiResponse<MovimientoResponse> get(@PathVariable UUID id, HttpServletRequest request) {
        return ok(MovimientoResponse.from(service.get(id)), request);
    }

    @GetMapping("/{id}/animales")
    ApiResponse<List<MovimientoDetalleResponse>> detalles(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.detalles(id).stream().map(MovimientoDetalleResponse::from).toList(), request);
    }

    @PostMapping("/{id}/confirmar")
    ApiResponse<MovimientoResponse> confirmar(@PathVariable UUID id, @Valid @RequestBody ConfirmarMovimientoRequest body,
                                              HttpServletRequest request) {
        return ok(MovimientoResponse.from(service.confirm(id, body.version())), request);
    }

    @PostMapping("/{id}/anular")
    ApiResponse<MovimientoResponse> anular(@PathVariable UUID id, @Valid @RequestBody AnularMovimientoRequest body,
                                           HttpServletRequest request) {
        return ok(MovimientoResponse.from(service.annul(id, body.motivo(), body.version())), request);
    }

    @PostMapping("/{id}/validar")
    ApiResponse<ValidacionMovimientoResponse> validar(@PathVariable UUID id, HttpServletRequest request) {
        return ok(ValidacionMovimientoResponse.from(service.validar(id)), request);
    }

    @PostMapping("/{id}/revertir")
    ApiResponse<MovimientoResponse> revertir(@PathVariable UUID id, @Valid @RequestBody RevertirMovimientoRequest body,
                                             HttpServletRequest request) {
        return ok(MovimientoResponse.from(service.revert(id, body.motivo(), body.version())), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
