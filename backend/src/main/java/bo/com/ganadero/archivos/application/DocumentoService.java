package bo.com.ganadero.archivos.application;

import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentoService {
    private final SupabaseStorageClient client;
    private final AppProperties properties;
    private final UserContext context;
    private final JdbcClient jdbc;

    public DocumentoService(SupabaseStorageClient client, AppProperties properties, UserContext context, JdbcClient jdbc) {
        this.client = client;
        this.properties = properties;
        this.context = context;
        this.jdbc = jdbc;
    }

    public PresignResult presign(PresignRequest request) {
        CurrentUser user = context.requirePermission("DOCUMENTO_SUBIR");
        String extension = extension(request.mimeType());
        String entidad = entidadFolder(request.entidadTipo());
        String path = "empresas/" + user.empresaId() + "/documentos/" + entidad + "/" + UUID.randomUUID() + "." + extension;
        return new PresignResult(path, path.substring(path.lastIndexOf('/') + 1),
                "/api/v1/sync/files/upload?path=" + path, "POST", request.mimeType(),
                properties.storage().signedUrlTtl().toSeconds());
    }

    public StoredUpload uploadFirma(MultipartFile file, String path) {
        CurrentUser user = context.requirePermission("DOCUMENTO_SUBIR");
        validate(file);
        validatePath(user, path);
        try {
            client.upload(path, file.getBytes(), file.getContentType());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        return new StoredUpload(path, client.signedUrl(path));
    }

    public DocumentoResponse uploadDocumento(MultipartFile file, String entidadTipo, UUID entidadId) {
        CurrentUser user = context.requirePermission("DOCUMENTO_SUBIR");
        validate(file);
        String extension = extension(file.getContentType());
        String entidad = entidadFolder(entidadTipo);
        String path = "empresas/" + user.empresaId() + "/documentos/" + entidad + "/" + UUID.randomUUID() + "." + extension;
        try {
            client.upload(path, file.getBytes(), file.getContentType());
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into archivos.documentos(id,empresa_id,entidad_tipo,entidad_id,nombre_original," +
                "nombre_almacenado,mime_type,tamano_bytes,created_by) " +
                "values(:id,:e,:tipo,:entidad,:original,:almacenado,:mime,:size,:actor)")
                .param("id", id).param("e", user.empresaId()).param("tipo", entidadTipo == null ? "GENERAL" : entidadTipo)
                .param("entidad", entidadId).param("original", file.getOriginalFilename()).param("almacenado", path)
                .param("mime", file.getContentType()).param("size", file.getSize()).param("actor", user.userId()).update();
        return new DocumentoResponse(id, entidadTipo == null ? "GENERAL" : entidadTipo, entidadId,
                file.getOriginalFilename(), path, file.getContentType(), file.getSize(), false,
                client.signedUrl(path), Instant.now());
    }

    public List<DocumentoResponse> list(String entidadTipo, UUID entidadId) {
        CurrentUser user = context.requirePermission("DOCUMENTO_VER");
        return jdbc.sql("""
                select * from archivos.documentos where empresa_id=:e
                and (:tipo is null or entidad_tipo=:tipo) and (:entidad is null or entidad_id=:entidad)
                order by created_at desc
                """).param("e", user.empresaId()).param("tipo", entidadTipo).param("entidad", entidadId)
                .query(this::map).list();
    }

    public void delete(UUID id) {
        CurrentUser user = context.requirePermission("DOCUMENTO_ELIMINAR");
        var optional = jdbc.sql("select * from archivos.documentos where id=:id and empresa_id=:e")
                .param("id", id).param("e", user.empresaId()).query(this::map).optional();
        if (optional.isEmpty()) throw new BusinessException(ErrorCode.DOCUMENTO_NOT_FOUND);
        client.delete(optional.get().nombreAlmacenado());
        jdbc.sql("delete from archivos.documentos where id=:id").param("id", id).update();
    }

    public String signedUrl(String path) {
        CurrentUser user = context.currentUser();
        String prefix = "empresas/" + user.empresaId() + "/documentos/";
        if (path == null || !path.startsWith(prefix) || path.contains("..")) throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        return client.signedUrl(path);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > properties.storage().maxBytes()
                || !properties.storage().allowedMimeTypes().contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (name.chars().filter(c -> c == '.').count() != 1
                || !properties.storage().allowedExtensions().contains(name.substring(name.lastIndexOf('.') + 1))) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
    }

    private void validatePath(CurrentUser user, String path) {
        String prefix = "empresas/" + user.empresaId() + "/documentos/";
        if (path == null || !path.startsWith(prefix) || path.contains("..") || path.contains(":")) {
            throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        }
    }

    private String entidadFolder(String entidadTipo) {
        return entidadTipo == null || entidadTipo.isBlank() ? "general" : entidadTipo.toLowerCase(Locale.ROOT);
    }

    private String extension(String mime) {
        return switch (mime) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        };
    }

    private DocumentoResponse map(ResultSet r, int row) throws SQLException {
        return new DocumentoResponse(r.getObject("id", UUID.class), r.getString("entidad_tipo"),
                r.getObject("entidad_id", UUID.class), r.getString("nombre_original"),
                r.getString("nombre_almacenado"), r.getString("mime_type"), r.getLong("tamano_bytes"),
                r.getBoolean("es_principal"), client.signedUrl(r.getString("nombre_almacenado")),
                r.getTimestamp("created_at").toInstant());
    }

    public record PresignRequest(@NotBlank String mimeType, String entidadTipo, UUID entidadId,
                                 @NotBlank String nombreOriginal, Long tamanoBytes) {
    }

    public record PresignResult(String path, String nombreAlmacenado, String uploadUrl, String metodo,
                                String mimeType, long expiresInSeconds) {
    }

    public record StoredUpload(String path, String signedUrl) {
    }

    public record DocumentoResponse(UUID id, String entidadTipo, UUID entidadId, String nombreOriginal,
            String nombreAlmacenado, String mimeType, long tamanoBytes, boolean esPrincipal, String url,
            Instant createdAt) {
    }
}
