package bo.com.ganadero;

import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.security.CurrentUser;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MigrationPostgisTest {

    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("postgis/postgis:16-3.4")
                    .asCompatibleSubstituteFor("postgres");

    private static final Path MIGRATIONS_DIRECTORY =
            Path.of(
                    "src",
                    "main",
                    "resources",
                    "db",
                    "migration"
            );

    private static final Pattern VERSIONED_MIGRATION_PATTERN =
            Pattern.compile("^V(.+?)__.+\\.sql$");

    @TempDir
    Path temporaryDirectory;

    @Test
    void appliesAllMigrationsToEmptyPostgisDatabase() throws Exception {
        try (PostgreSQLContainer<?> postgres = createPostgres()) {
            postgres.start();

            Flyway flyway = createFlyway(
                    postgres,
                    "classpath:db/migration"
            );

            MigrateResult firstMigration = flyway.migrate();

            assertThat(firstMigration.success).isTrue();
            assertThat(firstMigration.migrationsExecuted)
                    .isGreaterThan(0);

            assertThat(
                    flyway.info()
                            .current()
                            .getVersion()
                            .toString()
            ).isEqualTo("48");

            assertRequiredTablesExist(postgres);
            assertFlywayHistoryIsSuccessful(postgres);
            assertNoDuplicateTableNames(postgres);
            assertNoDuplicateIndexNames(postgres);
            assertMovementAuditColumnsExist(postgres);
            assertCodeGenerationIsAtomic(postgres);
            assertAlertIdempotencyConstraintExists(postgres);

            MigrateResult secondMigration = flyway.migrate();

            assertThat(secondMigration.success).isTrue();
            assertThat(secondMigration.migrationsExecuted).isZero();

            assertThat(
                    flyway.info()
                            .current()
                            .getVersion()
                            .toString()
            ).isEqualTo("48");
        }
    }

    private void assertCodeGenerationIsAtomic(PostgreSQLContainer<?> postgres) throws Exception {
        UUID empresaId = UUID.randomUUID();
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(),
                postgres.getPassword())) {
            connection.createStatement().executeUpdate("""
                    insert into core.empresas(id,codigo,razon_social,nombre_comercial)
                    values ('%s','CONC-001','Empresa concurrente','Empresa concurrente')
                    """.formatted(empresaId));
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(),
                postgres.getUsername(), postgres.getPassword());
        CodigoService codigos = new CodigoService(JdbcClient.create(dataSource));
        TransactionTemplate transactions = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        CurrentUser user = new CurrentUser(UUID.randomUUID(), empresaId, UUID.randomUUID(), Set.of(), Set.of(),
                Set.of(), true);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<String>> futures = java.util.stream.IntStream.range(0, 20)
                    .mapToObj(ignored -> executor.submit(() -> {
                        inicio.await();
                        return transactions.execute(status -> codigos.paraCreacion(user, TipoCodigo.ANIMAL,
                                null, null, null));
                    })).toList();
            inicio.countDown();
            Set<String> resultados = new HashSet<>();
            for (Future<String> future : futures) resultados.add(future.get());

            assertThat(resultados).hasSize(20).contains("ANI-000001", "ANI-000020");
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertAlertIdempotencyConstraintExists(PostgreSQLContainer<?> postgres) throws Exception {
        assertThat(queryForInt(postgres, """
                select count(*) from pg_indexes
                where schemaname='alertas' and indexname='uq_alerta_clave_idempotencia'
                """)).isEqualTo(1);
        assertThat(queryForInt(postgres, """
                select count(*) from information_schema.columns
                where table_schema='alertas' and table_name='alertas'
                  and column_name='clave_idempotencia' and is_nullable='NO'
                """)).isEqualTo(1);
    }

    @Test
    void upgradesExistingVersionNineDatabaseToLatestVersion()
            throws Exception {

        Path versionNineDirectory =
                temporaryDirectory.resolve("migrations-v9");

        copyMigrationsUpToVersion(
                MigrationVersion.fromVersion("9"),
                versionNineDirectory
        );

        try (PostgreSQLContainer<?> postgres = createPostgres()) {
            postgres.start();

            Flyway versionNineFlyway = createFlyway(
                    postgres,
                    filesystemLocation(versionNineDirectory)
            );

            MigrateResult versionNineResult =
                    versionNineFlyway.migrate();

            assertThat(versionNineResult.success).isTrue();

            assertThat(
                    versionNineFlyway.info()
                            .current()
                            .getVersion()
                            .toString()
            ).isEqualTo("9");

            insertVersionNineVerificationData(postgres);

            Flyway latestFlyway = createFlyway(
                    postgres,
                    "classpath:db/migration"
            );

            MigrateResult upgradeResult =
                    latestFlyway.migrate();

            assertThat(upgradeResult.success).isTrue();
            assertThat(upgradeResult.migrationsExecuted)
                    .isGreaterThan(0);

            assertThat(
                    latestFlyway.info()
                            .current()
                            .getVersion()
                            .toString()
            ).isEqualTo("48");

            assertVersionNineVerificationDataStillExists(postgres);
            assertRequiredTablesExist(postgres);
            assertMovementAuditColumnsExist(postgres);
            assertFlywayHistoryIsSuccessful(postgres);
            assertNoDuplicateTableNames(postgres);
            assertNoDuplicateIndexNames(postgres);

            MigrateResult repeatedUpgrade =
                    latestFlyway.migrate();

            assertThat(repeatedUpgrade.success).isTrue();
            assertThat(repeatedUpgrade.migrationsExecuted).isZero();
        }
    }

    @Test
    void upgradesLegacyAlertTypesAndPreferencesFromVersionFortyFive() throws Exception {
        Path versionFortyFiveDirectory = temporaryDirectory.resolve("migrations-v45");
        copyMigrationsUpToVersion(MigrationVersion.fromVersion("45"), versionFortyFiveDirectory);

        try (PostgreSQLContainer<?> postgres = createPostgres()) {
            postgres.start();
            Flyway versionFortyFive = createFlyway(postgres, filesystemLocation(versionFortyFiveDirectory));
            assertThat(versionFortyFive.migrate().success).isTrue();

            UUID empresa = UUID.randomUUID();
            UUID usuario = UUID.randomUUID();
            executeUpdate(postgres, """
                    insert into core.empresas(id,codigo,razon_social,nombre_comercial)
                    values (?,?,'Empresa alertas antiguas','Empresa alertas antiguas')
                    """, empresa, "ALT-" + empresa.toString().substring(0, 8));
            executeUpdate(postgres, """
                    insert into alertas.preferencias_notificacion(empresa_id,usuario_id)
                    values (?,?)
                    """, empresa, usuario);
            insertLegacyAlert(postgres, empresa, "PROXIMO_PARTO", "PARTO");
            insertLegacyAlert(postgres, empresa, "DIAGNOSTICO_GESTACION_PENDIENTE", "DIAGNOSTICO");
            insertLegacyAlert(postgres, empresa, "VACUNACION_PROXIMA", "VACUNA");
            insertLegacyAlert(postgres, empresa, "TRATAMIENTO_PENDIENTE", "TRATAMIENTO");
            insertLegacyAlert(postgres, empresa, "RETIRO_SANITARIO", "RETIRO");

            Flyway latest = createFlyway(postgres, "classpath:db/migration");
            assertThat(latest.migrate().success).isTrue();
            assertThat(latest.info().current().getVersion().toString()).isEqualTo("48");

            assertThat(queryForInt(postgres, """
                    select count(*) from alertas.alertas
                    where empresa_id=? and tipo in(
                        'PARTO_PROXIMO','DIAGNOSTICO_PENDIENTE','VACUNA_PROXIMA',
                        'TRATAMIENTO_PROXIMO','RETIRO_CARNE_VIGENTE'
                    )
                    """, empresa)).isEqualTo(5);
            assertThat(queryForInt(postgres, """
                    select count(*) from alertas.alertas
                    where empresa_id=? and tipo in(
                        'PROXIMO_PARTO','DIAGNOSTICO_GESTACION_PENDIENTE','VACUNACION_PROXIMA',
                        'TRATAMIENTO_PENDIENTE','RETIRO_SANITARIO'
                    )
                    """, empresa)).isZero();
            assertThat(queryForInt(postgres, """
                    select count(*) from alertas.preferencias_notificacion
                    where empresa_id=? and usuario_id=? and movimientos and inventario and sistema
                    """, empresa, usuario)).isEqualTo(1);
        }
    }

    private void insertLegacyAlert(PostgreSQLContainer<?> postgres, UUID empresa, String tipo, String origen)
            throws Exception {
        executeUpdate(postgres, """
                insert into alertas.alertas(
                    id,empresa_id,tipo,titulo,mensaje,severidad,fecha_programada,origen_tipo,origen_id,estado,metadata
                ) values (?, ?, ?, ?, 'Mensaje', 'WARNING', now(), ?, ?, 'PENDIENTE', '{}'::jsonb)
                """, UUID.randomUUID(), empresa, tipo, tipo, origen, UUID.randomUUID());
    }

    private PostgreSQLContainer<?> createPostgres() {
        return new PostgreSQLContainer<>(POSTGIS_IMAGE)
                .withDatabaseName("ganadero_migrations_test")
                .withUsername("ganadero_test")
                .withPassword("ganadero_test");
    }

    private Flyway createFlyway(
            PostgreSQLContainer<?> postgres,
            String location
    ) {
        return Flyway.configure()
                .dataSource(
                        postgres.getJdbcUrl(),
                        postgres.getUsername(),
                        postgres.getPassword()
                )
                .locations(location)
                .cleanDisabled(true)
                .validateMigrationNaming(true)
                .validateOnMigrate(true)
                .load();
    }

    private String filesystemLocation(Path directory) {
        return "filesystem:"
                + directory.toAbsolutePath()
                .normalize()
                .toString()
                .replace('\\', '/');
    }

    private void copyMigrationsUpToVersion(
            MigrationVersion maximumVersion,
            Path destination
    ) throws IOException {
        Files.createDirectories(destination);
        try (Stream<Path> files =
                     Files.list(MIGRATIONS_DIRECTORY)) {
            List<Path> migrations = files
                    .filter(Files::isRegularFile)
                    .filter(this::isVersionedMigration)
                    .filter(path ->
                            migrationVersion(path)
                                    .compareTo(maximumVersion) <= 0
                    )
                    .sorted(
                            Comparator.comparing(
                                    this::migrationVersion
                            )
                    )
                    .toList();
            assertThat(migrations)
                    .as(
                            "Migraciones encontradas hasta V%s",
                            maximumVersion
                    )
                    .isNotEmpty();
            for (Path migration : migrations) {
                Files.copy(
                        migration,
                        destination.resolve(
                                migration.getFileName().toString()
                        )
                );
            }
        }
    }

    private boolean isVersionedMigration(Path path) {
        return VERSIONED_MIGRATION_PATTERN
                .matcher(path.getFileName().toString())
                .matches();
    }

    private MigrationVersion migrationVersion(Path path) {
        String filename =
                path.getFileName().toString();
        Matcher matcher =
                VERSIONED_MIGRATION_PATTERN.matcher(filename);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Nombre de migraciÃƒÆ’Ã‚Â³n Flyway invÃƒÆ’Ã‚Â¡lido: "
                            + filename
            );
        }
        String version = matcher.group(1)
                .replace('_', '.');
        return MigrationVersion.fromVersion(version);
    }

    @Test
    void dashboardQueriesResolveAmbiguousColumns() throws Exception {
        try (PostgreSQLContainer<?> postgres = createPostgres()) {
            postgres.start();

            Flyway flyway = createFlyway(
                    postgres,
                    "classpath:db/migration"
            );

            assertThat(flyway.migrate().success).isTrue();

            UUID empresa =
                    UUID.fromString(
                            "00000000-0000-4000-8000-000000000001"
                    );

            assertThat(countRows(
                    postgres,
                    """
                    select coalesce(c.nombre, 'Sin categorÃ­a') as nombre, count(*) as total
                    from ganado.animales a
                    left join ganado.categorias_animal c on c.id = a.categoria_actual_id
                    where a.empresa_id = ? and a.estado = 'ACTIVO'
                    group by 1 order by total desc, 1
                    """,
                    empresa
            )).isZero();

            assertThat(countRows(
                    postgres,
                    """
                    select coalesce(p.nombre, 'Sin potrero') as nombre, count(*) as total
                    from ganado.animales a
                    left join campo.potreros p on p.id = a.potrero_actual_id
                    where a.empresa_id = ? and a.estado = 'ACTIVO'
                    group by 1 order by total desc, 1
                    """,
                    empresa
            )).isZero();

            assertThat(countRows(
                    postgres,
                    """
                    select coalesce(l.nombre, 'Sin lote') as nombre, count(*) as total
                    from ganado.animales a
                    left join ganado.lotes_ganaderos l on l.id = a.lote_actual_id
                    where a.empresa_id = ? and a.estado = 'ACTIVO'
                    group by 1 order by total desc, 1
                    """,
                    empresa
            )).isZero();

            assertThat(countRows(
                    postgres,
                    """
                    select p.id, p.animal_id, a.codigo as animal_codigo, a.nombre as animal_nombre, p.fecha, p.peso_kg
                    from produccion.pesajes p
                    join ganado.animales a on a.id = p.animal_id and a.empresa_id = p.empresa_id
                    where p.empresa_id = ? and p.estado = 'ACTIVO'
                    order by p.fecha desc, p.created_at desc limit ?
                    """,
                    empresa,
                    8
            )).isZero();
        }
    }

    @Test
    void auditoriaRegistrosSonInmutables() throws Exception {
        try (PostgreSQLContainer<?> postgres = createPostgres()) {
            postgres.start();

            Flyway flyway = createFlyway(
                    postgres,
                    "classpath:db/migration"
            );

            assertThat(flyway.migrate().success).isTrue();

            UUID id = UUID.randomUUID();
            UUID empresa = UUID.randomUUID();
            UUID usuario = UUID.randomUUID();
            executeUpdate(
                    postgres,
                    """
                    insert into core.empresas (
                        id,
                        codigo,
                        razon_social,
                        nombre_comercial,
                        estado,
                        created_at,
                        updated_at
                    )
                    values (?, ?, ?, ?, 'ACTIVA', now(), now())
                    """,
                    empresa,
                    "AUD-" + empresa.toString().substring(0, 8),
                    "Empresa auditorÃ­a inmutable S.R.L.",
                    "Empresa auditorÃ­a inmutable"
            );
            executeUpdate(
                    postgres,
                    """
                    insert into auditoria.registros
                        (id, empresa_id, usuario_id, accion, modulo, entidad, resultado, created_at)
                    values
                        (?, ?, ?, 'CREAR', 'ANIMALES', 'ANIMAL', 'EXITO', now())
                    """,
                    id,
                    empresa,
                    usuario
            );

            assertThatThrownBy(() -> executeUpdate(
                    postgres,
                    """
                    update auditoria.registros
                    set accion = 'ACTUALIZAR'
                    where id = ?
                    """,
                    id
            ))
                    .as("UPDATE de auditorÃ­a debe ser rechazado")
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("inmutables");

            assertThatThrownBy(() -> executeUpdate(
                    postgres,
                    "delete from auditoria.registros where id = ?",
                    id
            ))
                    .as("DELETE de auditorÃ­a debe ser rechazado")
                    .isInstanceOf(java.sql.SQLException.class)
                    .hasMessageContaining("inmutables");

            assertThat(countRows(
                    postgres,
                    "select id from auditoria.registros where id = ?",
                    id
            )).as("El registro debe seguir existiendo tras los intentos")
                    .isEqualTo(1);
        }
    }

    private int countRows(
            PostgreSQLContainer<?> postgres,
            String sql,
            Object... parameters
    ) throws Exception {
        try (
                Connection connection =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword()
                        );
                var statement =
                        connection.prepareStatement(sql)
        ) {
            for (
                    int index = 0;
                    index < parameters.length;
                    index++
            ) {
                statement.setObject(
                        index + 1,
                        parameters[index]
                );
            }
            try (ResultSet resultSet =
                         statement.executeQuery()) {
                int rows = 0;
                while (resultSet.next()) {
                    rows++;
                }
                return rows;
            }
        }
    }

    private void insertVersionNineVerificationData(
            PostgreSQLContainer<?> postgres
    ) throws Exception {
        executeUpdate(
                postgres,
                """
                insert into core.empresas (
                    id,
                    codigo,
                    razon_social,
                    nombre_comercial,
                    estado,
                    created_at,
                    updated_at
                )
                values (
                    '00000000-0000-0000-0000-000000000901',
                    'EMPRESA-MIGRACION-TEST',
                    'Empresa prueba migraciones S.R.L.',
                    'Empresa prueba migraciones',
                    'ACTIVA',
                    now(),
                    now()
                )
                """
        );
    }

    private void assertVersionNineVerificationDataStillExists(
            PostgreSQLContainer<?> postgres
    ) throws Exception {

        int count = queryForInt(
                postgres,
                """
                select count(*)
                from core.empresas
                where id =
                    '00000000-0000-0000-0000-000000000901'
                  and codigo = 'EMPRESA-MIGRACION-TEST'
                  and razon_social = 'Empresa prueba migraciones S.R.L.'
                  and nombre_comercial = 'Empresa prueba migraciones'
                  and estado = 'ACTIVA'
                """
        );

        assertThat(count)
                .as("La empresa creada en V9 debe sobrevivir hasta V30")
                .isEqualTo(1);
    }

    private void assertRequiredTablesExist(
            PostgreSQLContainer<?> postgres
    ) throws Exception {

        List<String> requiredTables = List.of(
                "core.empresas",
                "core.propiedades",
                "seguridad.perfiles_usuario",
                "seguridad.miembros_empresa",
                "seguridad.invitaciones_usuario",
                "seguridad.bootstrap_ejecuciones",
                "ganado.animales",
                "ganado.identificadores_animal",
                "ganado.parentescos",
                "ganado.lotes_ganaderos",
                "ganado.movimientos",
                "produccion.pesajes",
                "sync.dispositivos",
                "sync.operaciones",
                "sync.cambios",
                "archivos.documentos",
                "auditoria.registros"
        );

        for (String qualifiedTable : requiredTables) {
            String[] parts =
                    qualifiedTable.split("\\.", 2);

            int count = queryForInt(
                    postgres,
                    """
                    select count(*)
                    from information_schema.tables
                    where table_schema = ?
                      and table_name = ?
                    """,
                    parts[0],
                    parts[1]
            );

            assertThat(count)
                    .as(
                            "Debe existir la tabla %s",
                            qualifiedTable
                    )
                    .isEqualTo(1);
        }
    }

    private void assertMovementAuditColumnsExist(PostgreSQLContainer<?> postgres) throws Exception {
        int count = queryForInt(
                postgres,
                """
                select count(*)
                from information_schema.columns
                where table_schema = 'ganado'
                  and table_name = 'movimientos'
                  and column_name in ('created_by', 'updated_by')
                """
        );

        assertThat(count)
                .as("Los movimientos deben conservar las columnas de auditoria usadas por el repositorio")
                .isEqualTo(2);
    }

    private void assertFlywayHistoryIsSuccessful(
            PostgreSQLContainer<?> postgres
    ) throws Exception {

        int failedMigrations = queryForInt(
                postgres,
                """
                select count(*)
                from public.flyway_schema_history
                where success = false
                """
        );

        int versionTwentyFour = queryForInt(
                postgres,
                """
                select count(*)
                from public.flyway_schema_history
                where version = '25'
                  and success = true
                """
        );

        assertThat(failedMigrations).isZero();
        assertThat(versionTwentyFour).isEqualTo(1);
    }

    private void assertNoDuplicateTableNames(
            PostgreSQLContainer<?> postgres
    ) throws Exception {

        int duplicates = queryForInt(
                postgres,
                """
                select count(*)
                from (
                    select
                        table_schema,
                        table_name
                    from information_schema.tables
                    where table_schema not in (
                        'pg_catalog',
                        'information_schema'
                    )
                    group by
                        table_schema,
                        table_name
                    having count(*) > 1
                ) duplicated_tables
                """
        );

        assertThat(duplicates).isZero();
    }

    private void assertNoDuplicateIndexNames(
            PostgreSQLContainer<?> postgres
    ) throws Exception {

        int duplicates = queryForInt(
                postgres,
                """
                select count(*)
                from (
                    select
                        schemaname,
                        indexname
                    from pg_indexes
                    where schemaname not in (
                        'pg_catalog',
                        'information_schema'
                    )
                    group by
                        schemaname,
                        indexname
                    having count(*) > 1
                ) duplicated_indexes
                """
        );

        assertThat(duplicates).isZero();
    }

    private int queryForInt(
            PostgreSQLContainer<?> postgres,
            String sql,
            Object... parameters
    ) throws Exception {
        try (
                Connection connection =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword()
                        );
                var statement =
                        connection.prepareStatement(sql)
        ) {
            for (
                    int index = 0;
                    index < parameters.length;
                    index++
            ) {
                statement.setObject(
                        index + 1,
                        parameters[index]
                );
            }
            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getInt(1);
            }
        }
    }

    private void executeUpdate(
            PostgreSQLContainer<?> postgres,
            String sql,
            Object... parameters
    ) throws Exception {
        try (
                Connection connection =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword()
                        );
                var statement =
                        connection.prepareStatement(sql)
        ) {
            for (
                    int index = 0;
                    index < parameters.length;
                    index++
            ) {
                statement.setObject(
                        index + 1,
                        parameters[index]
                );
            }
            statement.executeUpdate();
        }
    }
}
