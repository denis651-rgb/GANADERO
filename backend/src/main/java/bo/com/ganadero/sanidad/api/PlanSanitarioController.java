package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.application.PlanSanitarioService;
import bo.com.ganadero.sanidad.application.ProximaActividadSanitaria;
import bo.com.ganadero.sanidad.domain.Enfermedad;
import bo.com.ganadero.sanidad.domain.PlanSanitario;
import bo.com.ganadero.sanidad.domain.PlanSanitarioItem;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sanidad")
public class PlanSanitarioController {

    private final PlanSanitarioService service;

    public PlanSanitarioController(PlanSanitarioService service) {
        this.service = service;
    }

    @GetMapping("/enfermedades")
    public ApiResponse<List<Enfermedad>> enfermedades(
            @RequestParam(defaultValue = "false") boolean incluirInactivas,
            HttpServletRequest request) {
        return ok(service.enfermedades(incluirInactivas), request);
    }

    @PostMapping("/enfermedades")
    public ApiResponse<Enfermedad> crear(@Valid @RequestBody CrearEnfermedadRequest body, HttpServletRequest request) {
        return ok(service.crearEnfermedad(body.command()), request);
    }

    @PatchMapping("/enfermedades/{id}/activo")
    public ApiResponse<Enfermedad> estado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarActivoRequest body,
            HttpServletRequest request) {
        return ok(service.estadoEnfermedad(id, body.activo()), request);
    }

    @GetMapping("/planes")
    public ApiResponse<List<PlanSanitario>> planes(HttpServletRequest request) {
        return ok(service.planes(), request);
    }

    @PostMapping("/planes")
    public ApiResponse<PlanSanitario> crearPlan(@Valid @RequestBody CrearPlanRequest body, HttpServletRequest request) {
        return ok(service.crearPlan(body.command()), request);
    }

    @PatchMapping("/planes/{id}/estado")
    public ApiResponse<PlanSanitario> estadoPlan(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoPlanRequest body,
            HttpServletRequest request) {
        return ok(service.cambiarEstado(id, body.estado(), body.version()), request);
    }

    @GetMapping("/planes/{id}/items")
    public ApiResponse<List<PlanSanitarioItem>> items(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "false") boolean incluirInactivos,
            HttpServletRequest request) {
        return ok(service.items(id, incluirInactivos), request);
    }

    @PostMapping("/planes/{id}/items")
    public ApiResponse<PlanSanitarioItem> crearItem(
            @PathVariable UUID id,
            @Valid @RequestBody CrearPlanItemRequest body,
            HttpServletRequest request) {
        return ok(service.crearItem(id, body.command()), request);
    }

    @PutMapping("/planes/{plan}/items/{id}")
    public ApiResponse<PlanSanitarioItem> actualizarItem(
            @PathVariable UUID plan,
            @PathVariable UUID id,
            @RequestParam long version,
            @Valid @RequestBody CrearPlanItemRequest body,
            HttpServletRequest request) {
        return ok(service.actualizarItem(plan, id, body.command(), version), request);
    }

    @GetMapping("/planes/{plan}/items/{id}/versiones")
    public ApiResponse<List<PlanSanitarioItem>> versiones(
            @PathVariable UUID plan,
            @PathVariable UUID id,
            HttpServletRequest request) {
        return ok(service.versiones(id), request);
    }

    @GetMapping("/planes/{id}/conflictos")
    public ApiResponse<List<PlanSanitario>> conflictos(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.conflictosActivacion(id), request);
    }

    @PatchMapping("/planes/{plan}/items/{id}/activo")
    public ApiResponse<PlanSanitarioItem> estadoItem(
            @PathVariable UUID plan,
            @PathVariable UUID id,
            @Valid @RequestBody CambiarActivoRequest body,
            HttpServletRequest request) {
        return ok(service.estadoItem(plan, id, body.activo(), body.version()), request);
    }

    @GetMapping("/planes/{plan}/items/{item}/proxima")
    public ApiResponse<ProximaActividadSanitaria> proxima(
            @PathVariable UUID plan,
            @PathVariable UUID item,
            @RequestParam LocalDate fechaAplicacion,
            HttpServletRequest request) {
        return ok(service.calcularProxima(plan, item, fechaAplicacion), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlationId == null ? "unknown" : correlationId.toString());
    }
}

