package bo.com.ganadero.seguridad.invitaciones;

import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios/invitaciones")
public class InvitacionController {

    private final InvitacionService service;

    public InvitacionController(InvitacionService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<InvitacionPage> listar(@RequestParam(required = false) String estado,
                                              @RequestParam(required = false) String email,
                                              @RequestParam(required = false) OffsetDateTime desde,
                                              @RequestParam(required = false) OffsetDateTime hasta,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              HttpServletRequest r) {
        return ok(service.listar(estado, email, desde, hasta, page, size), r);
    }

    @GetMapping("/{id}")
    public ApiResponse<InvitacionResponse> consultar(@PathVariable UUID id, HttpServletRequest r) {
        return ok(InvitacionResponse.from(service.consultar(id)), r);
    }

    @PostMapping
    public ApiResponse<InvitacionResponse> crear(@Valid @RequestBody CrearInvitacionRequest body, HttpServletRequest r) {
        return ok(InvitacionResponse.from(service.crear(body.email(), body.cargo())), r);
    }

    @PostMapping("/{id}/reenviar")
    public ApiResponse<InvitacionResponse> reenviar(@PathVariable UUID id,
                                                    @Valid @RequestBody ReenviarInvitacionRequest body,
                                                    HttpServletRequest r) {
        return ok(InvitacionResponse.from(service.reenviar(id, body.version())), r);
    }

    @PostMapping("/{id}/cancelar")
    public ApiResponse<InvitacionResponse> cancelar(@PathVariable UUID id,
                                                    @Valid @RequestBody CancelarInvitacionRequest body,
                                                    HttpServletRequest r) {
        return ok(InvitacionResponse.from(service.cancelar(id, body.motivo(), body.version())), r);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest r) {
        Object v = r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, v == null ? "unknown" : v.toString());
    }
}
