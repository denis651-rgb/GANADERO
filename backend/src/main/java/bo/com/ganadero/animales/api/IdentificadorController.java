package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.IdentificadorService;
import bo.com.ganadero.animales.application.QrService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/animales/{animalId}/identificadores")
public class IdentificadorController {
    private final IdentificadorService service;
    private final QrService qrs;

    public IdentificadorController(IdentificadorService service, QrService qrs) {
        this.service = service;
        this.qrs = qrs;
    }

    @GetMapping
    ApiResponse<List<IdentificadorResponse>> list(@PathVariable UUID animalId, HttpServletRequest request) {
        return ok(service.list(animalId).stream().map(IdentificadorResponse::from).toList(), request);
    }

    @PostMapping
    ApiResponse<IdentificadorResponse> assign(@PathVariable UUID animalId,
                                              @Valid @RequestBody AsignarIdentificadorRequest body,
                                              HttpServletRequest request) {
        return ok(IdentificadorResponse.from(service.assign(animalId, body.command())), request);
    }

    @PatchMapping("/{identificadorId}")
    ApiResponse<IdentificadorResponse> update(@PathVariable UUID animalId,
                                              @PathVariable UUID identificadorId,
                                              @Valid @RequestBody ActualizarIdentificadorRequest body,
                                              HttpServletRequest request) {
        return ok(IdentificadorResponse.from(service.update(animalId, identificadorId, body.command())), request);
    }

    @PostMapping("/{identificadorId}/retirar")
    ApiResponse<IdentificadorResponse> retire(@PathVariable UUID animalId,
                                              @PathVariable UUID identificadorId,
                                              @Valid @RequestBody RetirarIdentificadorRequest body,
                                              HttpServletRequest request) {
        return ok(IdentificadorResponse.from(service.retire(animalId, identificadorId, body.motivo(), body.version())), request);
    }

    @PostMapping("/{identificadorId}/principal")
    ApiResponse<IdentificadorResponse> principal(@PathVariable UUID animalId,
                                                 @PathVariable UUID identificadorId,
                                                 @Valid @RequestBody CambiarPrincipalRequest body,
                                                 HttpServletRequest request) {
        return ok(IdentificadorResponse.from(service.makePrincipal(animalId, identificadorId, body.version())), request);
    }

    @PostMapping("/qr")
    ApiResponse<IdentificadorResponse> generateQr(@PathVariable UUID animalId,
                                                  @Valid @RequestBody GenerarQrRequest body,
                                                  HttpServletRequest request) {
        boolean principal = body.principal() != null && body.principal();
        return ok(IdentificadorResponse.from(qrs.generate(animalId, principal)), request);
    }

    @PostMapping("/{identificadorId}/reemplazar-qr")
    ApiResponse<IdentificadorResponse> replaceQr(@PathVariable UUID animalId,
                                                 @PathVariable UUID identificadorId,
                                                 @Valid @RequestBody ReemplazarQrRequest body,
                                                 HttpServletRequest request) {
        return ok(IdentificadorResponse.from(
                qrs.replace(animalId, identificadorId, body.motivo(), body.principal(), body.version())), request);
    }

    @GetMapping("/{identificadorId}/qr")
    ResponseEntity<byte[]> qrImage(@PathVariable UUID animalId,
                                   @PathVariable UUID identificadorId,
                                   @RequestParam(defaultValue = "png") String format,
                                   @RequestParam(defaultValue = "512") int size,
                                   @RequestParam(defaultValue = "false") boolean download,
                                   HttpServletRequest request) {
        QrService.QrImageResult image = qrs.image(animalId, identificadorId, format, size);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(image.contentType()));
        headers.setCacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate());
        headers.set("X-Content-Type-Options", "nosniff");
        headers.setContentDispositionFormData("attachment", image.filename());
        if (!download) {
            headers.remove(HttpHeaders.CONTENT_DISPOSITION);
        }
        return ResponseEntity.ok().headers(headers).body(image.bytes());
    }

    private <T> ApiResponse<T> ok(T data, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
