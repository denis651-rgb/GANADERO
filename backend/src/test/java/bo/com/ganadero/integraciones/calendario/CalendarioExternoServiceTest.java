package bo.com.ganadero.integraciones.calendario;

import bo.com.ganadero.shared.security.LocalCurrentUserProvider;
import bo.com.ganadero.shared.security.UserContext;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.sqlite.SQLiteDataSource;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarioExternoServiceTest {

    @Test
    void conservaCorrespondenciaYReintentaSinDuplicarElTrabajo(@TempDir Path tempDir) {
        SQLiteDataSource ds = new SQLiteDataSource();
        ds.setUrl("jdbc:sqlite:" + tempDir.resolve("calendario-externo.db"));
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(ds);
        CalendarioExternoService service = new CalendarioExternoService(jdbc,
                new UserContext(new LocalCurrentUserProvider()));

        var config = service.guardar(new CalendarioExternoService.GuardarConfiguracion(
                "ganadero@example.com", "Ganadero - Sanidad", "America/La_Paz", true));
        assertThat(config.estado()).isEqualTo(EstadoConexionCalendario.PENDIENTE_AUTORIZACION);

        UUID ocurrencia = UUID.randomUUID();
        jdbc.sql("""
                insert into ocurrencia_calendario_sanitario
                    (id,plan_item_id,fecha_prevista,propiedad_id,potrero_id,ocurrencia_clave)
                values(:id,:item,:fecha,:propiedad,:potrero,:clave)
                """).param("id",ocurrencia.toString()).param("item",UUID.randomUUID().toString())
                .param("fecha",Instant.now().plusSeconds(3600).toString())
                .param("propiedad",UUID.randomUUID().toString()).param("potrero",UUID.randomUUID().toString())
                .param("clave","prueba-"+ocurrencia).update();

        service.confirmarConexion(new CalendarioExternoService.ConexionConfirmada(
                "ganadero@example.com", "calendar-id"));
        // Reconfirmar la conexión vuelve a recorrer pendientes, pero la clave idempotente conserva una sola fila.
        service.confirmarConexion(new CalendarioExternoService.ConexionConfirmada(
                "ganadero@example.com", "calendar-id"));
        assertThat(jdbc.sql("select count(*) from cola_sincronizacion_calendario")
                .query(Integer.class).single()).isEqualTo(1);

        List<TrabajoSincronizacionCalendario> reclamados = service.reclamar(10,"electron-pruebas");
        assertThat(reclamados).singleElement().satisfies(t -> {
            assertThat(t.estado()).isEqualTo(EstadoColaCalendario.PROCESANDO);
            assertThat(t.intentos()).isEqualTo(1);
        });
        UUID trabajo = reclamados.getFirst().id();
        var reintento = service.fallar(trabajo,
                new CalendarioExternoService.FalloSincronizacion("TEMPORAL","Sin conexión",true));
        assertThat(reintento.estado()).isEqualTo(EstadoColaCalendario.REINTENTO);

        jdbc.sql("update cola_sincronizacion_calendario set proximo_intento=:ahora where id=:id")
                .param("ahora",Instant.now().minusSeconds(1).toString()).param("id",trabajo.toString()).update();
        TrabajoSincronizacionCalendario segundo = service.reclamar(10,"electron-pruebas").getFirst();
        assertThat(segundo.intentos()).isEqualTo(2);

        var completado = service.completar(trabajo,new CalendarioExternoService.ResultadoExterno(
                "google-event-1","etag-1","https://calendar.google.com/event",Instant.now()));
        assertThat(completado.estado()).isEqualTo(EstadoColaCalendario.COMPLETADO);
        assertThat(jdbc.sql("select count(*) from correspondencia_calendario_externo where ocurrencia_id=:id")
                .param("id",ocurrencia.toString()).query(Integer.class).single()).isEqualTo(1);

        // El botón manual debe funcionar aunque el sondeo automático esté apagado.
        service.guardar(new CalendarioExternoService.GuardarConfiguracion(
                "ganadero@example.com", "Ganadero - Sanidad", "America/La_Paz", false));
        UUID ocurrenciaManual = UUID.randomUUID();
        jdbc.sql("""
                insert into ocurrencia_calendario_sanitario
                    (id,plan_item_id,fecha_prevista,propiedad_id,potrero_id,ocurrencia_clave)
                values(:id,:item,:fecha,:propiedad,:potrero,:clave)
                """).param("id",ocurrenciaManual.toString()).param("item",UUID.randomUUID().toString())
                .param("fecha",Instant.now().plusSeconds(7200).toString())
                .param("propiedad",UUID.randomUUID().toString()).param("potrero",UUID.randomUUID().toString())
                .param("clave","manual-"+ocurrenciaManual).update();
        service.prepararSincronizacionManual();
        assertThat(service.reclamar(10,"electron-pruebas",false)).isEmpty();
        assertThat(service.reclamar(10,"electron-pruebas",true))
                .extracting(TrabajoSincronizacionCalendario::ocurrenciaId)
                .containsExactly(ocurrenciaManual);

        var revocada = service.revocarAutorizacion();
        assertThat(revocada.estado()).isEqualTo(EstadoConexionCalendario.DESCONECTADO);
        assertThat(revocada.cuentaEmail()).isNull();
        assertThat(revocada.sincronizacionAutomatica()).isFalse();
    }
}
