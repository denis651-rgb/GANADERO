package bo.com.ganadero.respaldos.api;

import bo.com.ganadero.respaldos.application.RespaldoService;
import bo.com.ganadero.respaldos.domain.Respaldo;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/v1/respaldos")
public class RespaldoController {
    private final RespaldoService service;

    public RespaldoController(RespaldoService service) {
        this.service = service;
    }

    @PostMapping
    public ApiResponse<Respaldo> crear(HttpServletRequest request) {
        return success(service.crear(), request);
    }

    @GetMapping
    public ApiResponse<List<Respaldo>> listar(HttpServletRequest request) {
        return success(service.listar(), request);
    }

    @GetMapping("/{nombre}")
    public ApiResponse<Respaldo> obtener(@PathVariable String nombre, HttpServletRequest request) {
        return success(service.obtener(nombre), request);
    }

    @PostMapping("/{nombre}/verificar")
    public ApiResponse<Respaldo> verificar(@PathVariable String nombre, HttpServletRequest request) {
        return success(service.verificar(nombre), request);
    }

    @GetMapping("/{nombre}/descargar")
    public ResponseEntity<FileSystemResource> descargar(@PathVariable String nombre) {
        Path archivo = service.descargar(nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(archivo.getFileName().toString()).build().toString())
                .body(new FileSystemResource(archivo));
    }

    @DeleteMapping("/{nombre}")
    public ApiResponse<Void> eliminar(@PathVariable String nombre, HttpServletRequest request) {
        service.eliminar(nombre);
        return success(null, request);
    }

    @PatchMapping("/{nombre}/estado-externo")
    public ApiResponse<Respaldo> actualizarEstadoExterno(@PathVariable String nombre,
            @Valid @RequestBody ActualizarEstadoExternoRequest body, HttpServletRequest request) {
        return success(service.actualizarEstadoExterno(nombre, body.estado(), body.error()), request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
