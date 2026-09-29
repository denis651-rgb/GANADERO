package bo.com.ganadero.propiedades.api;

import bo.com.ganadero.propiedades.application.PropiedadService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PropiedadController {

    private final PropiedadService service;

    public PropiedadController(PropiedadService service) {
        this.service = service;
    }

    @GetMapping("/propiedades")
    public ApiResponse<List<PropiedadResponse>> list(HttpServletRequest request) {
        return ok(service.list().stream().map(PropiedadResponse::from).toList(), request);
    }

    @PostMapping("/propiedades")
    public ApiResponse<PropiedadResponse> create(@Valid @RequestBody CrearPropiedadRequest body, HttpServletRequest request) {
        return ok(PropiedadResponse.from(service.create(body.command())), request);
    }

    @GetMapping("/propiedades/{id}")
    public ApiResponse<PropiedadResponse> get(@PathVariable UUID id, HttpServletRequest request) {
        return ok(PropiedadResponse.from(service.get(id)), request);
    }

    @PatchMapping("/propiedades/{id}")
    public ApiResponse<PropiedadResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarPropiedadRequest body,
            HttpServletRequest request) {
        return ok(PropiedadResponse.from(service.update(id, body.command())), request);
    }

    @GetMapping("/propiedades/{id}/sectores")
    public ApiResponse<List<SectorResponse>> sectors(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.sectors(id).stream().map(SectorResponse::from).toList(), request);
    }

    @PostMapping("/propiedades/{id}/sectores")
    public ApiResponse<SectorResponse> createSector(
            @PathVariable UUID id,
            @Valid @RequestBody CrearSectorRequest body,
            HttpServletRequest request) {
        return ok(SectorResponse.from(service.createSector(id, body.command())), request);
    }

    @PatchMapping("/sectores/{id}")
    public ApiResponse<SectorResponse> updateSector(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarSectorRequest body,
            HttpServletRequest request) {
        return ok(SectorResponse.from(service.updateSector(id, body.command())), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlationId == null ? "unknown" : correlationId.toString());
    }
}

