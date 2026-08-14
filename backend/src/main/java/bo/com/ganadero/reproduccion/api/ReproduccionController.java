package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.application.ReproduccionService;
import bo.com.ganadero.reproduccion.application.ReproduccionCicloService;
import bo.com.ganadero.reproduccion.domain.*;
import bo.com.ganadero.reproduccion.domain.EstadoRegistroReproduccion;
import bo.com.ganadero.reproduccion.domain.IntensidadCelo;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/reproduccion")
public class ReproduccionController {
    private final ReproduccionService service;
    private final ReproduccionCicloService ciclo;

    public ReproduccionController(ReproduccionService service, ReproduccionCicloService ciclo) {
        this.service = service;
        this.ciclo = ciclo;
    }

    @PostMapping("/partos")
    ApiResponse<PartoResult> registrarParto(@Valid @RequestBody RegistrarPartoRequest body,HttpServletRequest request){
        return ok(ciclo.registrarParto(body.command()),request);
    }
    @GetMapping("/partos") ApiResponse<PartoPage> listarPartos(@RequestParam(required=false) UUID animalId,
      @RequestParam(required=false) UUID propiedadId,@RequestParam(defaultValue="0") @Min(0) int page,
      @RequestParam(defaultValue="20") @Min(1) @Max(500) int size,HttpServletRequest request){
        return ok(ciclo.listarPartos(animalId,propiedadId,page,size),request);}

    @PostMapping("/abortos")
    ApiResponse<Aborto> registrarAborto(@Valid @RequestBody RegistrarAbortoRequest body,HttpServletRequest request){
        return ok(ciclo.registrarAborto(body.command()),request);
    }
    @GetMapping("/abortos") ApiResponse<AbortoPage> listarAbortos(@RequestParam(required=false) UUID animalId,
      @RequestParam(required=false) UUID propiedadId,@RequestParam(defaultValue="0") @Min(0) int page,
      @RequestParam(defaultValue="20") @Min(1) @Max(500) int size,HttpServletRequest request){
        return ok(ciclo.listarAbortos(animalId,propiedadId,page,size),request);}

    @PostMapping("/destetes")
    ApiResponse<Destete> registrarDestete(@Valid @RequestBody RegistrarDesteteRequest body,HttpServletRequest request){
        return ok(ciclo.registrarDestete(body.command()),request);
    }
    @GetMapping("/destetes") ApiResponse<DestetePage> listarDestetes(@RequestParam(required=false) UUID animalId,
      @RequestParam(required=false) UUID propiedadId,@RequestParam(defaultValue="0") @Min(0) int page,
      @RequestParam(defaultValue="20") @Min(1) @Max(500) int size,HttpServletRequest request){
        return ok(ciclo.listarDestetes(animalId,propiedadId,page,size),request);}

    @GetMapping("/celos")
    ApiResponse<CeloPageResponse> listCelos(@RequestParam(required = false) UUID animalId,
                                            @RequestParam(required = false) Instant fechaDesde,
                                            @RequestParam(required = false) Instant fechaHasta,
                                            @RequestParam(required = false) IntensidadCelo intensidad,
                                            @RequestParam(required = false) EstadoRegistroReproduccion estado,
                                            @RequestParam(required = false) UUID propiedadId,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
                                            HttpServletRequest request) {
        return ok(CeloPageResponse.from(service.listCelos(animalId, fechaDesde, fechaHasta, intensidad,
                estado, propiedadId, page, size)), request);
    }

    @PostMapping("/celos")
    ApiResponse<CeloResponse> registrarCelo(@Valid @RequestBody RegistrarCeloRequest body, HttpServletRequest request) {
        return ok(CeloResponse.from(service.registrarCelo(body.command())), request);
    }

    @GetMapping("/celos/{id}")
    ApiResponse<CeloResponse> getCelo(@PathVariable UUID id, HttpServletRequest request) {
        return ok(CeloResponse.from(service.getCelo(id)), request);
    }

    @PostMapping("/celos/{id}/anular")
    ApiResponse<CeloResponse> anularCelo(@PathVariable UUID id,
                                         @Valid @RequestBody AnularReproduccionRequest body,
                                         HttpServletRequest request) {
        return ok(CeloResponse.from(service.anularCelo(id, body.motivo(), body.version())), request);
    }

    @GetMapping("/servicios")
    ApiResponse<ServicioPageResponse> listServicios(@RequestParam(required = false) UUID animalId,
                                                    @RequestParam(required = false) UUID propiedadId,
                                                    @RequestParam(defaultValue = "0") @Min(0) int page,
                                                    @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
                                                    HttpServletRequest request) {
        return ok(ServicioPageResponse.from(service.listServicios(animalId, propiedadId, page, size)), request);
    }

    @PostMapping("/servicios")
    ApiResponse<ServicioResponse> registrarServicio(@Valid @RequestBody RegistrarServicioRequest body, HttpServletRequest request) {
        return ok(ServicioResponse.from(service.registrarServicio(body.command())), request);
    }

    @GetMapping("/diagnosticos")
    ApiResponse<DiagnosticoPageResponse> listDiagnosticos(@RequestParam(required = false) UUID animalId,
                                                          @RequestParam(required = false) UUID propiedadId,
                                                          @RequestParam(defaultValue = "0") @Min(0) int page,
                                                          @RequestParam(defaultValue = "20") @Min(1) @Max(500) int size,
                                                          HttpServletRequest request) {
        return ok(DiagnosticoPageResponse.from(service.listDiagnosticos(animalId, propiedadId, page, size)), request);
    }

    @PostMapping("/diagnosticos")
    ApiResponse<DiagnosticoGestacionResponse> registrarDiagnostico(
            @Valid @RequestBody RegistrarDiagnosticoRequest body, HttpServletRequest request) {
        return ok(DiagnosticoGestacionResponse.from(service.registrarDiagnostico(body.command())), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
