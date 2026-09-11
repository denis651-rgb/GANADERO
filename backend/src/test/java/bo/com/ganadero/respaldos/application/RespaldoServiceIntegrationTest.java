package bo.com.ganadero.respaldos.application;

import bo.com.ganadero.respaldos.domain.EstadoRespaldo;
import bo.com.ganadero.respaldos.domain.IntegridadRespaldo;
import bo.com.ganadero.respaldos.domain.Respaldo;
import bo.com.ganadero.respaldos.infrastructure.JdbcRespaldoRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Corre las migraciones Flyway reales (incluida V37) contra un SQLite descartable y ejerce el
 * flujo completo de creación de respaldo (VACUUM INTO + PRAGMA integrity_check + SHA-256 + ZIP +
 * move atómico), mismo patrón que JdbcSanidadRepositoryTest/JdbcReporteRepositoryTest.
 */
class RespaldoServiceIntegrationTest {
    private RespaldoService service;
    private JdbcClient jdbc;
    private Path backupsDir;
    private UUID actorId;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        Path dbFile = tempDir.resolve("ganadero.db");
        DataSource dataSource = sqliteDataSource(dbFile);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        jdbc = JdbcClient.create(dataSource);
        backupsDir = tempDir.resolve("backups");

        actorId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(actorId, UUID.randomUUID(), UUID.randomUUID(), Set.of(),
                Set.of("RESPALDOS_VER", "RESPALDOS_CREAR", "RESPALDOS_ELIMINAR", "RESPALDOS_RESTAURAR", "RESPALDOS_CONFIGURAR"),
                Set.of(), true);
        service = new RespaldoService(new JdbcRespaldoRepository(jdbc), new UserContext(() -> user), new ObjectMapper(),
                jdbc, dbFile.toString(), backupsDir.toString(), "0.0.1-TEST");
    }

    @Test
    void creaUnRespaldoValidoConManifiestoYHashCorrectos() throws Exception {
        Respaldo creado = service.crear();

        assertThat(creado.nombreArchivo()).matches("Ganadero_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.ganadero-backup");
        assertThat(creado.estado()).isEqualTo(EstadoRespaldo.CREADO_LOCALMENTE);
        assertThat(creado.integridad()).isEqualTo(IntegridadRespaldo.VALIDA);

        Path archivo = backupsDir.resolve(creado.nombreArchivo());
        assertThat(archivo).exists();
        // No deben sobrevivir temporales.
        assertThat(Files.list(backupsDir).filter(p -> p.getFileName().toString().endsWith(".part")).count()).isZero();

        try (ZipFile zip = new ZipFile(archivo.toFile())) {
            ZipEntry manifestEntry = zip.getEntry("manifest.json");
            ZipEntry dbEntry = zip.getEntry("database/ganadero.db");
            assertThat(manifestEntry).isNotNull();
            assertThat(dbEntry).isNotNull();

            RespaldoManifest manifest = new ObjectMapper().readValue(zip.getInputStream(manifestEntry), RespaldoManifest.class);
            assertThat(manifest.formato()).isEqualTo("GANADERO_BACKUP");
            assertThat(manifest.zonaHoraria()).isEqualTo("America/La_Paz");
            assertThat(manifest.hashSha256()).isEqualTo(creado.hashSha256());

            Path extraido = backupsDir.resolve("extraido.db");
            Files.copy(zip.getInputStream(dbEntry), extraido);
            assertThat(sha256(extraido)).isEqualToIgnoringCase(creado.hashSha256());
            assertThat(pragmaIntegrityCheck(extraido)).isTrue();
        }
    }

    @Test
    void unaSegundaCreacionConcurrenteEsRechazada() throws Exception {
        // ReentrantLock es reentrante para el mismo hilo: hay que sostenerlo desde OTRO hilo para
        // simular de verdad una segunda petición HTTP concurrente.
        ReentrantLock lock = obtenerLockInterno();
        CountDownLatch tomado = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);
        Thread otraOperacion = new Thread(() -> {
            lock.lock();
            tomado.countDown();
            try {
                liberar.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        otraOperacion.start();
        tomado.await();
        try {
            assertThatThrownBy(() -> service.crear())
                    .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RESPALDO_OPERACION_EN_CURSO));
        } finally {
            liberar.countDown();
            otraOperacion.join();
        }
        // Liberado el lock, una creación normal vuelve a funcionar.
        assertThat(service.crear().estado()).isEqualTo(EstadoRespaldo.CREADO_LOCALMENTE);
    }

    @Test
    void detectaUnaDiscrepanciaDeHashComoIntegridadInvalida() {
        Respaldo creado = service.crear();
        jdbc.sql("update respaldo_registro set hash_sha256='0000000000000000000000000000000000000000000000000000000000000000' where nombre_archivo=:n")
                .param("n", creado.nombreArchivo()).update();

        Respaldo verificado = service.verificar(creado.nombreArchivo());

        assertThat(verificado.integridad()).isEqualTo(IntegridadRespaldo.INVALIDA);
        assertThat(verificado.estado()).isEqualTo(EstadoRespaldo.INTEGRIDAD_INVALIDA);
    }

    @Test
    void noPermiteEliminarElUltimoRespaldoValidoPeroSiUnoDeVarios() {
        Respaldo primero = service.crear();
        esperarUnSegundo();
        Respaldo segundo = service.crear();

        service.eliminar(primero.nombreArchivo());
        List<Respaldo> restantes = service.listar();
        assertThat(restantes).extracting(Respaldo::nombreArchivo).containsExactly(segundo.nombreArchivo());

        assertThatThrownBy(() -> service.eliminar(segundo.nombreArchivo()))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.RESPALDO_ES_EL_UNICO_VALIDO));
    }

    @Test
    void descargaDevuelveLaRutaRealDelArchivo() {
        Respaldo creado = service.crear();
        Path descargado = service.descargar(creado.nombreArchivo());
        assertThat(descargado).exists().hasFileName(creado.nombreArchivo());
    }

    // Los nombres de respaldo incluyen segundos; dos creaciones en el mismo segundo colisionarían.
    private void esperarUnSegundo() {
        try {
            Thread.sleep(1100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ReentrantLock obtenerLockInterno() throws Exception {
        Field field = RespaldoService.class.getDeclaredField("lock");
        field.setAccessible(true);
        return (ReentrantLock) field.get(service);
    }

    private String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(Files.readAllBytes(file));
        return HexFormat.of().formatHex(digest.digest());
    }

    private boolean pragmaIntegrityCheck(Path dbFile) throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA integrity_check")) {
            return rs.next() && "ok".equalsIgnoreCase(rs.getString(1));
        }
    }

    private DataSource sqliteDataSource(Path dbFile) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + dbFile + "?foreign_keys=on");
        return dataSource;
    }
}
