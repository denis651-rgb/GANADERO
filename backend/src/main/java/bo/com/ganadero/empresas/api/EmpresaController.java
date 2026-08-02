package bo.com.ganadero.empresas.api;

import bo.com.ganadero.empresas.application.*;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empresa")
public class EmpresaController {
    private final ConsultarEmpresaUseCase consultarEmpresa;
    private final ActualizarEmpresaUseCase actualizarEmpresa;
    private final ConsultarConfiguracionUseCase consultarConfiguracion;
    private final ActualizarConfiguracionUseCase actualizarConfiguracion;

    public EmpresaController(ConsultarEmpresaUseCase consultarEmpresa, ActualizarEmpresaUseCase actualizarEmpresa,
            ConsultarConfiguracionUseCase consultarConfiguracion,
            ActualizarConfiguracionUseCase actualizarConfiguracion) {
        this.consultarEmpresa = consultarEmpresa; this.actualizarEmpresa = actualizarEmpresa;
        this.consultarConfiguracion = consultarConfiguracion;
        this.actualizarConfiguracion = actualizarConfiguracion;
    }

    @GetMapping
    public ApiResponse<EmpresaResponse> get(HttpServletRequest request) {
        return ApiResponse.success(EmpresaResponse.from(consultarEmpresa.execute()), correlationId(request));
    }

    @PatchMapping
    public ApiResponse<EmpresaResponse> update(@Valid @RequestBody ActualizarEmpresaRequest body,
                                                HttpServletRequest request) {
        return ApiResponse.success(EmpresaResponse.from(actualizarEmpresa.execute(body.toCommand())),
                correlationId(request));
    }

    @GetMapping("/configuracion")
    public ApiResponse<ConfiguracionEmpresaResponse> getConfiguration(HttpServletRequest request) {
        return ApiResponse.success(ConfiguracionEmpresaResponse.from(consultarConfiguracion.execute()),
                correlationId(request));
    }

    @PatchMapping("/configuracion")
    public ApiResponse<ConfiguracionEmpresaResponse> updateConfiguration(
            @Valid @RequestBody ActualizarConfiguracionRequest body, HttpServletRequest request) {
        return ApiResponse.success(ConfiguracionEmpresaResponse.from(
                actualizarConfiguracion.execute(body.toCommand())), correlationId(request));
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return value == null ? "unknown" : value.toString();
    }
}
