package bo.com.ganadero.sync.api;

import bo.com.ganadero.archivos.application.DocumentoService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {
    private final bo.com.ganadero.sync.application.SyncService service;
    private final DocumentoService documentos;

    public SyncController(bo.com.ganadero.sync.application.SyncService service, DocumentoService documentos) {
        this.service = service;
        this.documentos = documentos;
    }

    @PostMapping("/dispositivo")
    ApiResponse<SyncDispositivoResponse> registrarDispositivo(@Valid @RequestBody SyncPushRequest.DispositivoInfo body,
                                                              HttpServletRequest request) {
        return ok(service.registrarDispositivo(body), request);
    }

    @PostMapping("/push")
    ApiResponse<SyncPushResponse> push(@Valid @RequestBody SyncPushRequest body, HttpServletRequest request) {
        return ok(service.push(body), request);
    }

    @GetMapping("/pull")
    ApiResponse<SyncPullResponse> pull(@RequestParam("dispositivo") @NotBlank String dispositivo,
                                       @RequestParam(defaultValue = "0") @Min(0) long cursor,
                                       @RequestParam(defaultValue = "200") @Min(1) @Max(2000) int size,
                                       @RequestParam(required = false) String plataforma,
                                       @RequestParam(required = false) String versionApp,
                                       HttpServletRequest request) {
        SyncPushRequest.DispositivoInfo info = new SyncPushRequest.DispositivoInfo(dispositivo, null, plataforma, versionApp);
        return ok(service.pull(info, cursor, size), request);
    }

    @GetMapping("/bootstrap")
    ApiResponse<SyncBootstrapResponse> bootstrap(@RequestParam("dispositivo") @NotBlank String dispositivo,
                                                 @RequestParam(required = false) String plataforma,
                                                 @RequestParam(required = false) String versionApp,
                                                 HttpServletRequest request) {
        SyncPushRequest.DispositivoInfo info = new SyncPushRequest.DispositivoInfo(dispositivo, null, plataforma, versionApp);
        return ok(service.bootstrap(info), request);
    }

    @PostMapping("/files/presign")
    ApiResponse<DocumentoService.PresignResult> presign(@Valid @RequestBody DocumentoService.PresignRequest body,
                                                        HttpServletRequest request) {
        return ok(documentos.presign(body), request);
    }

    @PostMapping(value = "/files/upload", consumes = "multipart/form-data")
    ApiResponse<DocumentoService.StoredUpload> uploadFirma(@RequestPart("file") MultipartFile file,
            @RequestParam("path") @NotBlank String path, HttpServletRequest request) {
        return ok(documentos.uploadFirma(file, path), request);
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
