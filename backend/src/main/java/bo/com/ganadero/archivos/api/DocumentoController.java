package bo.com.ganadero.archivos.api;

import bo.com.ganadero.archivos.application.DocumentoService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/archivos/documentos")
public class DocumentoController {
    private final DocumentoService service;

    public DocumentoController(DocumentoService service) {
        this.service = service;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<DocumentoService.DocumentoResponse> upload(@RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String entidadTipo, @RequestParam(required = false) UUID entidadId,
            HttpServletRequest request) {
        return success(service.uploadDocumento(file, entidadTipo, entidadId), request);
    }

    @GetMapping
    public ApiResponse<List<DocumentoService.DocumentoResponse>> list(@RequestParam(required = false) String entidadTipo,
            @RequestParam(required = false) UUID entidadId, HttpServletRequest request) {
        return success(service.list(entidadTipo, entidadId), request);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id, HttpServletRequest request) {
        service.delete(id);
        return success(null, request);
    }

    private <T> ApiResponse<T> success(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
