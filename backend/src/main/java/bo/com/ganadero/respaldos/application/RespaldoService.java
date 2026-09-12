package bo.com.ganadero.respaldos.application;

import bo.com.ganadero.respaldos.domain.EstadoRespaldo;
import bo.com.ganadero.respaldos.domain.IntegridadRespaldo;
import bo.com.ganadero.respaldos.domain.Respaldo;
import bo.com.ganadero.respaldos.domain.RespaldoRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Crea, lista, verifica, descarga y elimina respaldos de la base SQLite (archivos
 * {@code .ganadero-backup}, un ZIP con {@code manifest.json} + {@code database/ganadero.db}).
 *
 * <p>La creación usa {@code VACUUM INTO} sobre la conexión principal (fuera de cualquier
 * transacción Spring/JPA, requisito de SQLite) para obtener un snapshot consistente sin copiar
 * el archivo abierto directamente. Un {@link ReentrantLock} evita que dos creaciones corran a
 * la vez. Junto a la base se empaqueta también la carpeta de media (fotos de animales, ver
 * {@code app.storage.root-path}) bajo la entrada {@code media/}, para que un respaldo restaurado
 * en otra instalación no pierda las fotos. La copia a una carpeta externa sincronizada (p. ej.
 * Google Drive de escritorio) y la restauración las hace Electron por fuera de este servicio —
 * este service nunca detiene su propio proceso ni reemplaza el .db que tiene abierto.</p>
 */
@Service
public class RespaldoService {
    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^Ganadero_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.ganadero-backup$");
    private static final DateTimeFormatter NOMBRE_FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");
    // v2 agrega la carpeta media/ al paquete; v1 (solo manifest.json + database/ganadero.db)
    // se sigue leyendo/restaurando sin problema, simplemente no trae fotos.
    private static final int VERSION_FORMATO = 2;
    private static final String ENTRADA_MANIFEST = "manifest.json";
    private static final String ENTRADA_BASE = "database/ganadero.db";
    private static final String ENTRADA_MEDIA_PREFIJO = "media/";

    private final RespaldoRepository respaldos;
    private final UserContext context;
    private final ObjectMapper objectMapper;
    private final JdbcClient jdbc;
    private final Path backupsDir;
    private final Path mediaDir;
    private final String versionAplicacion;
    private final ReentrantLock lock = new ReentrantLock();

    public RespaldoService(RespaldoRepository respaldos, UserContext context, ObjectMapper objectMapper,
                           JdbcClient jdbc,
                           @Value("${GANADERO_DB_PATH:./data/ganadero.db}") String dbPath,
                           @Value("${GANADERO_BACKUPS_PATH:}") String backupsPathRaw,
                           @Value("${app.storage.root-path:./data/media}") String mediaPathRaw,
                           @Value("${info.app.version}") String versionAplicacion) {
        this.respaldos = respaldos;
        this.context = context;
        this.objectMapper = objectMapper;
        this.jdbc = jdbc;
        this.versionAplicacion = versionAplicacion;
        this.backupsDir = resolverCarpetaRespaldos(dbPath, backupsPathRaw);
        this.mediaDir = Path.of(mediaPathRaw).toAbsolutePath().normalize();
    }

    private static Path resolverCarpetaRespaldos(String dbPath, String backupsPathRaw) {
        if (backupsPathRaw != null && !backupsPathRaw.isBlank()) {
            return Path.of(backupsPathRaw).toAbsolutePath().normalize();
        }
        Path dbParent = Path.of(dbPath).toAbsolutePath().normalize().getParent();
        return (dbParent == null ? Path.of(".") : dbParent).resolve("backups");
    }

    @Transactional(readOnly = true)
    public List<Respaldo> listar() {
        context.requirePermission("RESPALDOS_VER");
        return respaldos.listar();
    }

    /** Detalle de un respaldo puntual; es el paso que Electron llama (con el backend aún arriba) antes de restaurar. */
    @Transactional(readOnly = true)
    public Respaldo obtener(String nombre) {
        context.requirePermission("RESPALDOS_RESTAURAR");
        return buscarORequerido(nombre);
    }

    public Respaldo crear() {
        CurrentUser user = context.requirePermission("RESPALDOS_CREAR");
        if (!lock.tryLock()) throw new BusinessException(ErrorCode.RESPALDO_OPERACION_EN_CURSO);
        try {
            return crearInternal(user);
        } finally {
            lock.unlock();
        }
    }

    private Respaldo crearInternal(CurrentUser user) {
        try {
            Files.createDirectories(backupsDir);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.RESPALDO_CREACION_FALLIDA, "No se pudo crear la carpeta de respaldos.");
        }

        String nombreArchivo = "Ganadero_" + ZonedDateTime.now(BOLIVIA).format(NOMBRE_FORMATO) + ".ganadero-backup";
        Path destinoFinal = backupsDir.resolve(nombreArchivo);
        if (Files.exists(destinoFinal)) throw new BusinessException(ErrorCode.RESPALDO_YA_EXISTE);

        Path dbTemporal = backupsDir.resolve(UUID.randomUUID() + ".db.part");
        Path paqueteTemporal = backupsDir.resolve(nombreArchivo + ".part");
        try {
            ejecutarVacuumInto(dbTemporal);
            if (!integridadOk(dbTemporal)) {
                throw new BusinessException(ErrorCode.RESPALDO_INTEGRIDAD_INVALIDA,
                        "El snapshot generado no pasó PRAGMA integrity_check.");
            }
            String hash = calcularSha256(dbTemporal);
            long tamano = Files.size(dbTemporal);
            String versionBaseDatos = obtenerVersionFlyway();
            RespaldoManifest manifest = new RespaldoManifest("GANADERO_BACKUP", VERSION_FORMATO, versionAplicacion,
                    versionBaseDatos, Instant.now().toString(), "America/La_Paz",
                    user.empresaId() == null ? null : user.empresaId().toString(), ENTRADA_BASE, tamano, hash);
            empaquetar(paqueteTemporal, dbTemporal, manifest);
            Files.move(paqueteTemporal, destinoFinal, StandardCopyOption.ATOMIC_MOVE);

            Respaldo registro = new Respaldo(nombreArchivo, Instant.now(), tamano, hash, VERSION_FORMATO,
                    versionAplicacion, versionBaseDatos, user.empresaId(), EstadoRespaldo.CREADO_LOCALMENTE,
                    IntegridadRespaldo.VALIDA, null, null, user.userId(), Instant.now());
            return respaldos.crear(registro);
        } catch (BusinessException e) {
            limpiar(dbTemporal, paqueteTemporal, destinoFinal);
            throw e;
        } catch (Exception e) {
            limpiar(dbTemporal, paqueteTemporal, destinoFinal);
            throw new BusinessException(ErrorCode.RESPALDO_CREACION_FALLIDA, e.getMessage());
        } finally {
            limpiar(dbTemporal);
        }
    }

    @Transactional
    public Respaldo verificar(String nombre) {
        context.requirePermission("RESPALDOS_VER");
        Respaldo registro = buscarORequerido(nombre);
        Path archivo = resolverArchivo(nombre);
        if (!Files.exists(archivo)) {
            throw new BusinessException(ErrorCode.RESPALDO_NOT_FOUND, "El archivo del respaldo ya no existe en disco.");
        }
        Path temporal = backupsDir.resolve(UUID.randomUUID() + ".verificacion.db");
        try {
            extraerEntrada(archivo, ENTRADA_BASE, temporal);
            boolean integro = integridadOk(temporal) && calcularSha256(temporal).equalsIgnoreCase(registro.hashSha256());
            respaldos.actualizarIntegridad(nombre, integro ? IntegridadRespaldo.VALIDA : IntegridadRespaldo.INVALIDA);
            if (!integro) {
                respaldos.actualizarEstado(nombre, EstadoRespaldo.INTEGRIDAD_INVALIDA,
                        "La verificación de integridad no coincide con el manifiesto.", null);
            }
            return buscarORequerido(nombre);
        } catch (IOException e) {
            respaldos.actualizarIntegridad(nombre, IntegridadRespaldo.INVALIDA);
            throw new BusinessException(ErrorCode.RESPALDO_INTEGRIDAD_INVALIDA, "No se pudo leer el respaldo: " + e.getMessage());
        } finally {
            limpiar(temporal);
        }
    }

    @Transactional(readOnly = true)
    public Path descargar(String nombre) {
        context.requirePermission("RESPALDOS_VER");
        buscarORequerido(nombre);
        Path archivo = resolverArchivo(nombre);
        if (!Files.exists(archivo)) throw new BusinessException(ErrorCode.RESPALDO_NOT_FOUND);
        return archivo;
    }

    public void eliminar(String nombre) {
        context.requirePermission("RESPALDOS_ELIMINAR");
        Respaldo registro = buscarORequerido(nombre);
        if (registro.integridad() == IntegridadRespaldo.DESCONOCIDA) {
            throw new BusinessException(ErrorCode.RESPALDO_INTEGRIDAD_INVALIDA,
                    "Verifica la integridad antes de eliminar este respaldo.");
        }
        boolean enProgreso = registro.estado() == EstadoRespaldo.CREANDO
                || registro.estado() == EstadoRespaldo.COPIANDO_A_CARPETA_EXTERNA;
        if (enProgreso) throw new BusinessException(ErrorCode.RESPALDO_OPERACION_EN_CURSO);
        if (registro.integridad() == IntegridadRespaldo.VALIDA && respaldos.contarValidos() <= 1) {
            throw new BusinessException(ErrorCode.RESPALDO_ES_EL_UNICO_VALIDO);
        }
        limpiar(resolverArchivo(nombre));
        respaldos.eliminar(nombre);
    }

    /** Solo lo invoca Electron, para reportar el resultado de copiar el respaldo a la carpeta sincronizada. */
    public Respaldo actualizarEstadoExterno(String nombre, EstadoRespaldo estado, String error) {
        context.requirePermission("RESPALDOS_CONFIGURAR");
        buscarORequerido(nombre);
        respaldos.actualizarEstado(nombre, estado, error,
                estado == EstadoRespaldo.COPIADO_A_CARPETA_EXTERNA ? Instant.now() : null);
        return buscarORequerido(nombre);
    }

    private Respaldo buscarORequerido(String nombre) {
        validarNombre(nombre);
        return respaldos.buscar(nombre).orElseThrow(() -> new BusinessException(ErrorCode.RESPALDO_NOT_FOUND));
    }

    private void validarNombre(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new BusinessException(ErrorCode.RESPALDO_NOMBRE_INVALIDO);
        }
    }

    /** Doble candado contra path traversal: regex estricta del nombre + normalize()/startsWith de la ruta resuelta. */
    private Path resolverArchivo(String nombre) {
        validarNombre(nombre);
        Path candidato = backupsDir.resolve(nombre).normalize();
        if (!candidato.startsWith(backupsDir)) throw new BusinessException(ErrorCode.RESPALDO_NOMBRE_INVALIDO);
        return candidato;
    }

    private void ejecutarVacuumInto(Path destino) {
        String ruta = destino.toAbsolutePath().toString().replace("'", "''");
        jdbc.sql("VACUUM INTO '" + ruta + "'").update();
    }

    private boolean integridadOk(Path dbFile) {
        String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        try (Connection conn = DriverManager.getConnection(url);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
            return rs.next() && "ok".equalsIgnoreCase(rs.getString(1));
        } catch (SQLException e) {
            return false;
        }
    }

    private String calcularSha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash del respaldo.", e);
        }
    }

    private String obtenerVersionFlyway() {
        try {
            return jdbc.sql("select version from flyway_schema_history where success=1 order by installed_rank desc limit 1")
                    .query(String.class).optional().orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private void empaquetar(Path zipDestino, Path dbFile, RespaldoManifest manifest) {
        try (OutputStream fos = Files.newOutputStream(zipDestino);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zos.putNextEntry(new ZipEntry(ENTRADA_MANIFEST));
            zos.write(objectMapper.writeValueAsBytes(manifest));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(ENTRADA_BASE));
            Files.copy(dbFile, zos);
            zos.closeEntry();
            empaquetarMedia(zos);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo empaquetar el respaldo.", e);
        }
    }

    /** Sin carpeta de media (instalación nueva sin fotos aún) simplemente no agrega entradas. */
    private void empaquetarMedia(ZipOutputStream zos) throws IOException {
        if (!Files.isDirectory(mediaDir)) return;
        try (Stream<Path> archivos = Files.walk(mediaDir)) {
            for (Path archivo : archivos.filter(Files::isRegularFile).sorted().toList()) {
                String relativo = mediaDir.relativize(archivo).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(ENTRADA_MEDIA_PREFIJO + relativo));
                Files.copy(archivo, zos);
                zos.closeEntry();
            }
        }
    }

    private void extraerEntrada(Path zip, String entryName, Path destino) throws IOException {
        try (ZipFile zf = new ZipFile(zip.toFile())) {
            ZipEntry entry = zf.getEntry(entryName);
            if (entry == null) throw new IOException("El respaldo no contiene " + entryName);
            try (InputStream in = zf.getInputStream(entry)) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private void limpiar(Path... archivos) {
        for (Path archivo : archivos) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignored) {
                // best-effort: no dejar un archivo a medio escribir no debe ocultar el error original
            }
        }
    }
}
