package bo.com.ganadero.archivos.application;

import bo.com.ganadero.animales.application.AnimalService;
import bo.com.ganadero.shared.config.AppProperties;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final TimelineEventPublisher timeline;
    private final ApplicationEventPublisher events;
    private final AnimalService animales;
    private final ImagenValidador validador;

    public DocumentoService(SupabaseStorageClient client, AppProperties properties, UserContext context,
                            JdbcClient jdbc, TimelineEventPublisher timeline, ApplicationEventPublisher events,
                            AnimalService animales, ImagenValidador validador) {
        this.client = client;
        this.properties = properties;
        this.context = context;
        this.jdbc = jdbc;
        this.timeline = timeline;
        this.events = events;
        this.animales = animales;
        this.validador = validador;
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
        byte[] content = bytes(file);
        validate(file, content);
        validatePath(user, path);
        client.upload(path, content, file.getContentType());
        return new StoredUpload(path, client.signedUrl(path));
    }

    @Transactional
    public DocumentoResponse uploadDocumento(MultipartFile file, String entidadTipo, UUID entidadId) {
        return uploadDocumento(file, entidadTipo, entidadId, false);
    }

    @Transactional
    public DocumentoResponse uploadDocumento(MultipartFile file, String entidadTipo, UUID entidadId, boolean principal) {
        CurrentUser user = context.requirePermission("DOCUMENTO_SUBIR");
        byte[] content = bytes(file);
        validate(file, content);
        ImagenValidador.Resultado dimensiones = validador.validar(content, file.getContentType());
        boolean animal = entidadId != null && "ANIMAL".equalsIgnoreCase(entidadTipo);
        if (principal && !animal) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION);
        String extension = extension(file.getContentType());
        String entidad = entidadFolder(entidadTipo);
        String path = "empresas/" + user.empresaId() + "/documentos/" + entidad + "/" + UUID.randomUUID() + "." + extension;
        client.upload(path, content, file.getContentType());
        UUID id = UUID.randomUUID();
        jdbc.sql("insert into archivos.documentos(id,empresa_id,entidad_tipo,entidad_id,nombre_original," +
                "nombre_almacenado,mime_type,tamano_bytes,es_principal,ancho_px,alto_px,created_by) " +
                "values(:id,:e,:tipo,:entidad,:original,:almacenado,:mime,:size,:principal,:ancho,:alto,:actor)")
                .param("id", id).param("e", user.empresaId()).param("tipo", entidadTipo == null ? "GENERAL" : entidadTipo)
                .param("entidad", entidadId).param("original", file.getOriginalFilename()).param("almacenado", path)
                .param("mime", file.getContentType()).param("size", file.getSize()).param("principal", principal)
                .param("ancho", dimensiones.ancho()).param("alto", dimensiones.alto()).param("actor", user.userId()).update();
        if (animal) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("nombreOriginal", file.getOriginalFilename());
            metadata.put("mimeType", file.getContentType());
            metadata.put("tamanoBytes", file.getSize());
            metadata.put("anchoPx", dimensiones.ancho());
            metadata.put("altoPx", dimensiones.alto());
            metadata.put("esPrincipal", principal);
            timeline.publish(new RegistrarEventoTimeline(user.empresaId(), entidadId, TipoEventoAnimal.FOTO_AGREGADA,
                    null, "Se agregó la fotografía " + file.getOriginalFilename() + ".", null, id, metadata,
                    user.userId(), Instant.now(), null));
            if (principal) {
                establecerPrincipal(user, entidadId, id, file.getOriginalFilename());
            }
        }
        audit(user, "SUBIR_FOTO", id, Map.of("animalId", entidadId == null ? null : entidadId.toString(),
                "principal", principal));
        return new DocumentoResponse(id, entidadTipo == null ? "GENERAL" : entidadTipo, entidadId,
                file.getOriginalFilename(), path, file.getContentType(), file.getSize(), principal,
                dimensiones.ancho(), dimensiones.alto(), user.userId(), null, 0,
                client.signedUrl(path), Instant.now());
    }

    @Transactional
    public DocumentoResponse markPrincipal(UUID id) {
        CurrentUser user = context.requirePermission("DOCUMENTO_SUBIR");
        DocumentoResponse doc = require(id, user.empresaId());
        if (doc.esPrincipal()) return doc;
        if (!"ANIMAL".equalsIgnoreCase(doc.entidadTipo()) || doc.entidadId() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION);
        }
        establecerPrincipal(user, doc.entidadId(), doc.id(), doc.nombreOriginal());
        audit(user, "MARCAR_PRINCIPAL", id, Map.of("animalId", doc.entidadId().toString()));
        return require(id, user.empresaId());
    }

    private void establecerPrincipal(CurrentUser user, UUID animalId, UUID fotoId, String nombreOriginal) {
        jdbc.sql("update archivos.documentos set es_principal=false where empresa_id=:e and entidad_tipo='ANIMAL' and entidad_id=:animal and es_principal=true")
                .param("e", user.empresaId()).param("animal", animalId).update();
        jdbc.sql("update archivos.documentos set es_principal=true,updated_at=now(),version=version+1 where id=:id")
                .param("id", fotoId).update();
        String path = jdbc.sql("select nombre_almacenado from archivos.documentos where id=:id")
                .param("id", fotoId).query(String.class).single();
        animales.asignarFotoPrincipal(animalId, path);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fotoId", fotoId.toString());
        metadata.put("nombreOriginal", nombreOriginal);
        metadata.put("esPrincipal", true);
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), animalId, TipoEventoAnimal.FOTO_PRINCIPAL_CAMBIADA,
                null, "La fotografía " + nombreOriginal + " es ahora la principal.", null, fotoId, metadata,
                user.userId(), Instant.now(), null));
    }

    public List<DocumentoResponse> list(String entidadTipo, UUID entidadId) {
        CurrentUser user = context.requirePermission("DOCUMENTO_VER");
        return jdbc.sql("""
                select d.*, nullif(trim(concat(coalesce(pu.nombres,''),' ',coalesce(pu.apellidos,''))),'') as usuario_nombre
                from archivos.documentos d
                left join seguridad.perfiles_usuario pu on pu.id = d.created_by
                where d.empresa_id=:e
                and (:tipo is null or d.entidad_tipo=:tipo) and (:entidad is null or d.entidad_id=:entidad)
                order by d.created_at desc
                """).param("e", user.empresaId()).param("tipo", entidadTipo).param("entidad", entidadId)
                .query(this::map).list();
    }

    @Transactional
    public void delete(UUID id, boolean confirmarPrincipal) {
        CurrentUser user = context.requirePermission("DOCUMENTO_ELIMINAR");
        DocumentoResponse doc = require(id, user.empresaId());
        if (doc.esPrincipal() && !confirmarPrincipal) {
            throw new BusinessException(ErrorCode.FOTO_PRINCIPAL_CONFIRMATION_REQUIRED);
        }
        client.delete(doc.nombreAlmacenado());
        jdbc.sql("delete from archivos.documentos where id=:id").param("id", id).update();
        if (doc.esPrincipal() && doc.entidadId() != null) {
            animales.limpiarFotoPrincipal(doc.entidadId());
        }
        if (doc.entidadId() != null && "ANIMAL".equalsIgnoreCase(doc.entidadTipo())) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("fotoId", id.toString());
            metadata.put("nombreOriginal", doc.nombreOriginal());
            metadata.put("esPrincipal", doc.esPrincipal());
            timeline.publish(new RegistrarEventoTimeline(user.empresaId(), doc.entidadId(),
                    TipoEventoAnimal.FOTO_ELIMINADA,
                    null, "Se eliminó la fotografía " + doc.nombreOriginal() + ".", null, id, metadata,
                    user.userId(), Instant.now(), null));
        }
        audit(user, "ELIMINAR_FOTO", id, Map.of("esPrincipal", doc.esPrincipal()));
    }

    public String signedUrl(String path) {
        CurrentUser user = context.currentUser();
        String prefix = "empresas/" + user.empresaId() + "/documentos/";
        if (path == null || !path.startsWith(prefix) || path.contains("..")) throw new BusinessException(ErrorCode.PROPERTY_ACCESS_DENIED);
        return client.signedUrl(path);
    }

    private DocumentoResponse require(UUID id, UUID empresa) {
        return jdbc.sql("""
                select d.*, nullif(trim(concat(coalesce(pu.nombres,''),' ',coalesce(pu.apellidos,''))),'') as usuario_nombre
                from archivos.documentos d
                left join seguridad.perfiles_usuario pu on pu.id = d.created_by
                where d.id=:id and d.empresa_id=:e
                """).param("id", id).param("e", empresa).query(this::map)
                .optional().orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENTO_NOT_FOUND));
    }

    private byte[] bytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
    }

    private void validate(MultipartFile file, byte[] content) {
        if (file == null || file.isEmpty() || file.getSize() > properties.storage().maxBytes()
                || !properties.storage().allowedMimeTypes().contains(file.getContentType())) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        if (name.chars().filter(c -> c == '.').count() != 1
                || !properties.storage().allowedExtensions().contains(name.substring(name.lastIndexOf('.') + 1))) {
            throw new BusinessException(ErrorCode.STORAGE_FILE_INVALID);
        }
        validador.validar(content, file.getContentType());
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

    private void audit(CurrentUser user, String accion, UUID id, Map<String, Object> datos) {
        events.publishEvent(new ArchivoAuditEvent(user.empresaId(), user.userId(), accion, "DOCUMENTO", id,
                Instant.now(), datos));
    }

    private DocumentoResponse map(ResultSet r, int row) throws SQLException {
        return new DocumentoResponse(r.getObject("id", UUID.class), r.getString("entidad_tipo"),
                r.getObject("entidad_id", UUID.class), r.getString("nombre_original"),
                r.getString("nombre_almacenado"), r.getString("mime_type"), r.getLong("tamano_bytes"),
                r.getBoolean("es_principal"), r.getObject("ancho_px", Integer.class),
                r.getObject("alto_px", Integer.class), r.getObject("created_by", UUID.class),
                r.getString("usuario_nombre"), r.getLong("version"), client.signedUrl(r.getString("nombre_almacenado")),
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
            String nombreAlmacenado, String mimeType, long tamanoBytes, boolean esPrincipal, Integer anchoPx,
            Integer altoPx, UUID createdBy, String usuarioNombre, long version, String url, Instant createdAt) {
    }
}
