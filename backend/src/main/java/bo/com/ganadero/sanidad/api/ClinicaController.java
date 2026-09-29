package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.application.ClinicaService;
import bo.com.ganadero.sanidad.domain.AplicacionTratamiento;
import bo.com.ganadero.sanidad.domain.CasoClinico;
import bo.com.ganadero.sanidad.domain.ControlEctoparasitario;
import bo.com.ganadero.sanidad.domain.ControlNeonatal;
import bo.com.ganadero.sanidad.domain.ExamenReproductivo;
import bo.com.ganadero.sanidad.domain.Tratamiento;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sanidad")
public class ClinicaController {

    private final ClinicaService service;

    public ClinicaController(ClinicaService service) {
        this.service = service;
    }

    @PostMapping("/casos-clinicos")
    public ApiResponse<CasoClinico> crearCaso(@Valid @RequestBody CrearCasoClinicoRequest body, HttpServletRequest request) {
        return ok(service.crearCaso(body.command()), request);
    }

    @GetMapping("/casos-clinicos")
    public ApiResponse<List<CasoClinico>> casos(@RequestParam(required = false) UUID animalId, HttpServletRequest request) {
        return ok(service.casos(animalId), request);
    }

    @PostMapping("/casos-clinicos/{id}/cerrar")
    public ApiResponse<CasoClinico> cerrar(
            @PathVariable UUID id,
            @Valid @RequestBody CerrarCasoClinicoRequest body,
            HttpServletRequest request) {
        return ok(service.cerrarCaso(id, body.resultado()), request);
    }

    @PostMapping("/tratamientos")
    public ApiResponse<Tratamiento> crear(@Valid @RequestBody CrearTratamientoRequest body, HttpServletRequest request) {
        return ok(service.crearTratamiento(body.command()), request);
    }

    @GetMapping("/tratamientos")
    public ApiResponse<List<Tratamiento>> tratamientos(@RequestParam(required = false) UUID animalId, HttpServletRequest request) {
        return ok(service.tratamientos(animalId), request);
    }

    @PostMapping("/tratamientos/{id}/activar")
    public ApiResponse<List<AplicacionTratamiento>> activar(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.activar(id), request);
    }

    @PostMapping("/tratamientos/{id}/regenerar")
    public ApiResponse<List<AplicacionTratamiento>> regenerar(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.regenerar(id), request);
    }

    @GetMapping("/tratamientos/{id}/aplicaciones")
    public ApiResponse<List<AplicacionTratamiento>> aplicaciones(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.aplicaciones(id), request);
    }

    @PostMapping("/tratamientos/{tratamientoId}/aplicaciones/{aplicacionId}/aplicar")
    public ApiResponse<AplicacionTratamiento> aplicar(
            @PathVariable UUID tratamientoId,
            @PathVariable UUID aplicacionId,
            @Valid @RequestBody AplicarTratamientoRequest body,
            HttpServletRequest request) {
        return ok(service.aplicar(tratamientoId, aplicacionId, body.command()), request);
    }

    @PostMapping("/tratamientos/aplicaciones/marcar-atrasadas")
    public ApiResponse<List<AplicacionTratamiento>> atrasadas(HttpServletRequest request) {
        return ok(service.marcarAtrasadas(), request);
    }

    @PostMapping("/tratamientos/{id}/finalizar")
    public ApiResponse<Tratamiento> finalizar(@PathVariable UUID id, HttpServletRequest request) {
        return ok(service.finalizar(id), request);
    }

    @PostMapping("/control-neonatal")
    public ApiResponse<ControlNeonatal> crearControl(@Valid @RequestBody CrearControlNeonatalRequest body, HttpServletRequest request) {
        return ok(service.crearControl(body.command()), request);
    }

    @GetMapping("/control-neonatal")
    public ApiResponse<List<ControlNeonatal>> controlesNeonatales(@RequestParam(required = false) UUID animalId, HttpServletRequest request) {
        return ok(service.controlesNeonatales(animalId), request);
    }

    @PostMapping("/control-ectoparasitario")
    public ApiResponse<ControlEctoparasitario> crearControlEcto(@Valid @RequestBody CrearControlEctoparasitarioRequest body, HttpServletRequest request) {
        return ok(service.crearControlEcto(body.command()), request);
    }

    @GetMapping("/control-ectoparasitario")
    public ApiResponse<List<ControlEctoparasitario>> controlesEcto(
            @RequestParam(required = false) UUID animalId,
            @RequestParam(required = false) UUID loteGanaderoId,
            HttpServletRequest request) {
        return ok(service.controlesEctoparasitarios(animalId, loteGanaderoId), request);
    }

    @GetMapping("/control-ectoparasitario/principios-recientes")
    public ApiResponse<List<String>> principiosRecientes(
            @RequestParam(required = false) UUID animalId,
            @RequestParam(required = false) UUID loteGanaderoId,
            HttpServletRequest request) {
        return ok(service.principiosActivosRecientes(animalId, loteGanaderoId), request);
    }

    @PostMapping("/examenes-reproductivos")
    public ApiResponse<ExamenReproductivo> crearExamen(@Valid @RequestBody CrearExamenReproductivoRequest body, HttpServletRequest request) {
        return ok(service.crearExamen(body.command()), request);
    }

    @GetMapping("/examenes-reproductivos")
    public ApiResponse<List<ExamenReproductivo>> examenesReproductivos(@RequestParam(required = false) UUID animalId, HttpServletRequest request) {
        return ok(service.examenesReproductivos(animalId), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, correlationId == null ? "unknown" : correlationId.toString());
    }
}

