package bo.com.ganadero.alertas.api;
import bo.com.ganadero.alertas.application.RecordatorioService;
import bo.com.ganadero.alertas.domain.Recordatorio;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/alertas/recordatorios")
public class RecordatorioController{
 private final RecordatorioService service;public RecordatorioController(RecordatorioService service){this.service=service;}
 @GetMapping public ApiResponse<List<Recordatorio>> listar(HttpServletRequest r){return ok(service.listar(),r);}
 @PostMapping public ApiResponse<Recordatorio> crear(@Valid @RequestBody CrearRecordatorioRequest body,HttpServletRequest r){return ok(service.crear(body.command()),r);}
 @PostMapping("/{id}/pausar") public ApiResponse<Recordatorio> pausar(@PathVariable UUID id,@RequestParam long version,HttpServletRequest r){return ok(service.pausar(id,version),r);}
 @PostMapping("/{id}/reanudar") public ApiResponse<Recordatorio> reanudar(@PathVariable UUID id,@RequestParam long version,HttpServletRequest r){return ok(service.reanudar(id,version),r);}
 @PostMapping("/{id}/cancelar") public ApiResponse<Recordatorio> cancelar(@PathVariable UUID id,@RequestParam long version,HttpServletRequest r){return ok(service.cancelar(id,version),r);}
 private<T>ApiResponse<T>ok(T data,HttpServletRequest r){Object c=r.getAttribute(CorrelationIdFilter.ATTRIBUTE);return ApiResponse.success(data,c==null?"unknown":c.toString());}
}
