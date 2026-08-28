package bo.com.ganadero.archivos.infrastructure;

import bo.com.ganadero.archivos.application.FileStorageClient;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Almacena archivos en disco local (carpeta de datos de la app de escritorio),
 * reemplaza al antiguo cliente de Supabase Storage en la nube.
 */
@Component
public class LocalFileStorageClient implements FileStorageClient {
    private final Path root;

    public LocalFileStorageClient(AppProperties properties) {
        this.root = Path.of(properties.storage().rootPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento local: " + root, exception);
        }
    }

    @Override
    public void upload(String path, byte[] content, String contentType) {
        Path target = resolve(path);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    @Override
    public String url(String path) {
        resolve(path);
        return "/api/v1/media/" + path;
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.STORAGE_UNAVAILABLE);
        }
    }

    private Path resolve(String path) {
        if (path == null || path.isBlank() || path.contains("..")) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        Path target = root.resolve(path).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        return target;
    }
}
