package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.application.*;
import bo.com.ganadero.sanidad.domain.JornadaSanitaria;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jornadas-sanitarias")
public class JornadaSanitariaController {
    private final JornadaSanitariaService service;

    public JornadaSanitariaController(JornadaSanitariaService service) { this.service = service; }

    @PostMapping ApiResponse<JornadaSanitaria> crear(@Valid @RequestBody CrearJornadaRequest body, HttpServletRequest request) { return ok(service.crear(body.command()), request); }
    @PutMapping("/{id}") ApiResponse<JornadaSanitaria> actualizar(@PathVariable UUID id, @Valid @RequestBody ActualizarJornadaRequest body, HttpServletRequest request) { return ok(service.actualizar(id, body.command()), request); }
    @PostMapping("/{id}/anular") ApiResponse<JornadaSanitaria> anular(@PathVariable UUID id, @Valid @RequestBody AnularJornadaRequest body, HttpServletRequest request) { return ok(service.anular(id, body.version(), body.motivo()), request); }
    @GetMapping ApiResponse<List<JornadaSanitaria>> listar(HttpServletRequest request) { return ok(service.listar(), request); }
    @GetMapping("/animales-elegibles") ApiResponse<List<Animal>> elegibles(@RequestParam UUID propiedadId, @RequestParam(required=false) UUID loteId, @RequestParam(required=false) UUID categoriaId, @RequestParam(required=false) SexoAnimal sexo, HttpServletRequest request) { return ok(service.elegibles(propiedadId,loteId,categoriaId,sexo),request); }
    @GetMapping("/{id}/elegibilidad") ApiResponse<ResultadoElegibilidad> elegibilidad(@PathVariable UUID id, @RequestParam UUID planItemId, @RequestParam LocalDate fechaAplicacion, HttpServletRequest request) { return ok(service.elegibilidad(id,planItemId,fechaAplicacion),request); }
    @PutMapping("/{id}/animales") ApiResponse<List<UUID>> seleccionar(@PathVariable UUID id, @Valid @RequestBody SeleccionAnimalesRequest body, HttpServletRequest request) { return ok(service.seleccionar(id,body.planItemId(),body.fechaAplicacion(),body.animalIds()),request); }
    @PostMapping("/{id}/confirmar") ApiResponse<ConfirmacionJornadaResult> confirmar(@PathVariable UUID id, @Valid @RequestBody ConfirmarJornadaRequest body, HttpServletRequest request) { return ok(service.confirmar(id,body.command()),request); }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) { Object correlationId=request.getAttribute(CorrelationIdFilter.ATTRIBUTE); return ApiResponse.success(data,correlationId==null?"unknown":correlationId.toString()); }
}
