package bo.com.ganadero.integraciones.calendario;

import bo.com.ganadero.sanidad.application.CalendarioSanitarioService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/integraciones/calendario")
public class CalendarioExternoController {
    private final CalendarioExternoService service;
    private final CalendarioSanitarioService calendarioSanitario;
    public CalendarioExternoController(CalendarioExternoService service,
                                       CalendarioSanitarioService calendarioSanitario) {
        this.service=service;
        this.calendarioSanitario=calendarioSanitario;
    }

    @GetMapping public ApiResponse<ConfiguracionCalendarioExterno> consultar(HttpServletRequest r) {
        return ok(service.consultar(),r);
    }
    @PutMapping public ApiResponse<ConfiguracionCalendarioExterno> guardar(
            @RequestBody CalendarioExternoService.GuardarConfiguracion body,HttpServletRequest r) {
        return ok(service.guardar(body),r);
    }
    @PostMapping("/electron/cola/reclamar")
    public ApiResponse<List<TrabajoSincronizacionCalendario>> reclamar(
            @RequestBody ReclamarColaRequest body,HttpServletRequest r) {
        return ok(service.reclamar(body.limite()==null?10:body.limite(),body.dispositivoId(),
                Boolean.TRUE.equals(body.manual())),r);
    }
    @PostMapping("/electron/sincronizar/preparar")
    public ApiResponse<PreparacionSincronizacion> prepararSincronizacion(HttpServletRequest r) {
        int eventosGenerados = calendarioSanitario.procesar();
        return ok(new PreparacionSincronizacion(eventosGenerados, service.prepararSincronizacionManual()),r);
    }
    @PostMapping("/electron/conexion/confirmar")
    public ApiResponse<ConfiguracionCalendarioExterno> confirmarConexion(
            @RequestBody CalendarioExternoService.ConexionConfirmada body,HttpServletRequest r) {
        return ok(service.confirmarConexion(body),r);
    }
    @PostMapping("/electron/oauth/autorizada")
    public ApiResponse<ConfiguracionCalendarioExterno> autorizada(
            @RequestBody CalendarioExternoService.AutorizacionConfirmada body,HttpServletRequest r) {
        return ok(service.registrarAutorizacion(body),r);
    }
    @PostMapping("/electron/oauth/revocada")
    public ApiResponse<ConfiguracionCalendarioExterno> revocada(HttpServletRequest r) {
        return ok(service.revocarAutorizacion(),r);
    }
    @GetMapping("/electron/estado")
    public ApiResponse<CalendarioExternoService.ResumenCola> estado(HttpServletRequest r) {
        return ok(service.resumenCola(),r);
    }
    @PostMapping("/reintentar")
    public ApiResponse<CalendarioExternoService.ResumenCola> reintentar(HttpServletRequest r) {
        return ok(service.reintentarErrores(),r);
    }
    @GetMapping("/ocurrencias")
    public ApiResponse<List<CalendarioExternoService.EstadoOcurrencia>> ocurrencias(HttpServletRequest r) {
        return ok(service.listarOcurrencias(),r);
    }
    @PostMapping("/ocurrencias/{id}/reintentar")
    public ApiResponse<Void> reintentarOcurrencia(@PathVariable UUID id,HttpServletRequest r) {
        service.reintentarOcurrencia(id); return ok(null,r);
    }
    @PostMapping("/electron/cola/{id}/completar")
    public ApiResponse<TrabajoSincronizacionCalendario> completar(@PathVariable UUID id,
            @RequestBody CalendarioExternoService.ResultadoExterno body,HttpServletRequest r) {
        return ok(service.completar(id,body),r);
    }
    @PostMapping("/electron/cola/{id}/fallar")
    public ApiResponse<TrabajoSincronizacionCalendario> fallar(@PathVariable UUID id,
            @RequestBody CalendarioExternoService.FalloSincronizacion body,HttpServletRequest r) {
        return ok(service.fallar(id,body),r);
    }
    private <T> ApiResponse<T> ok(T data,HttpServletRequest r) {
        Object id=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data,id==null?"unknown":id.toString());
    }
    public record ReclamarColaRequest(String dispositivoId,Integer limite,Boolean manual) {}
    public record PreparacionSincronizacion(int eventosGenerados, CalendarioExternoService.ResumenCola estado) {}
}
