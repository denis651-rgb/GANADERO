package bo.com.ganadero.archivos.api;

import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sirve los archivos guardados en disco local por {@link bo.com.ganadero.archivos.infrastructure.LocalFileStorageClient}.
 */
@RestController
public class MediaController {
    private static final String PREFIX = "/api/v1/media/";
    private final Path root;

    public MediaController(AppProperties properties) {
        this.root = Path.of(properties.storage().rootPath()).toAbsolutePath().normalize();
    }

    @GetMapping("/api/v1/media/**")
    public ResponseEntity<FileSystemResource> get(HttpServletRequest request) {
        String uri = request.getRequestURI();
        int index = uri.indexOf(PREFIX);
        String raw = index < 0 ? "" : uri.substring(index + PREFIX.length());
        String relative = UriUtils.decode(raw, StandardCharsets.UTF_8);
        if (relative.isBlank() || relative.contains("..")) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root) || !Files.isRegularFile(target)) {
            throw new BusinessException(ErrorCode.DOCUMENTO_NOT_FOUND);
        }
        String contentType;
        try {
            contentType = Files.probeContentType(target);
        } catch (IOException exception) {
            contentType = null;
        }
        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)))
                .body(new FileSystemResource(target));
    }
}
