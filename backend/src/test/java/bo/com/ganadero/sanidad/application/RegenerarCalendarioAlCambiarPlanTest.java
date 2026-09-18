package bo.com.ganadero.sanidad.application;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/**
 * Contexto Spring real con SQLite, transacciones reales y el mismo pool de <b>una sola conexión</b>
 * que usa la app de escritorio ({@code application-local.yml}). Lo delicado no es qué hace
 * {@code procesar()} sino cuándo corre (solo tras confirmar el guardado) y que no se cuelgue ni
 * pierda lo que escribe con ese pool, algo que un mock del publicador no puede demostrar.
 */
class RegenerarCalendarioAlCambiarPlanTest {
    private static final CalendarioSanitarioService CALENDARIO = mock(CalendarioSanitarioService.class);

    private AnnotationConfigApplicationContext contexto;
    private JdbcClient jdbc;
    private JdbcClient otraConexion;
    private Cambios cambios;

    @BeforeEach
    void iniciar(@TempDir Path dir) {
        reset(CALENDARIO);
        String url = "jdbc:sqlite:" + dir.resolve("prueba.db");
        System.setProperty("prueba.db.url", url);
        contexto = new AnnotationConfigApplicationContext(Config.class);
        jdbc = contexto.getBean(JdbcClient.class);
        jdbc.sql("create table marca(dato text)").update();
        jdbc.sql("create table plan_guardado(dato text)").update();
        SQLiteDataSource independiente = new SQLiteDataSource();
        independiente.setUrl(url);
        otraConexion = JdbcClient.create(independiente);
        cambios = contexto.getBean(Cambios.class);
    }

    @AfterEach
    void cerrar() throws InterruptedException {
        // Deja terminar el hilo de generación antes de cerrar: en Windows el archivo aún abierto impide borrar el directorio temporal.
        Thread.sleep(400);
        contexto.close();
    }

    @Test
    void generaElCalendarioTrasConfirmarSinColgarseYSuEscrituraQuedaConfirmada() throws InterruptedException {
        AtomicBoolean guardadoVisibleAlGenerar = new AtomicBoolean();
        doAnswer(inv -> {
            // La actividad ya está confirmada y visible desde otra conexión cuando empieza la generación...
            guardadoVisibleAlGenerar.set(contar("plan_guardado") == 1);
            // ...y lo que escribe procesar() con el pool de una conexión debe quedar confirmado.
            jdbc.sql("insert into marca(dato) values ('calendario generado')").update();
            return 0;
        }).when(CALENDARIO).procesar();

        cambios.guardarActividad();

        verify(CALENDARIO, timeout(5000)).procesar();
        esperarA(() -> contar("marca") == 1);
        assertThat(contar("marca")).isEqualTo(1);
        assertThat(guardadoVisibleAlGenerar).isTrue();
    }

    @Test
    void noGeneraNadaSiLaOperacionSeRevierte() throws InterruptedException {
        assertThatThrownBy(() -> cambios.guardarYFallar()).isInstanceOf(IllegalStateException.class);

        Thread.sleep(300);
        verify(CALENDARIO, never()).procesar();
        assertThat(contar("plan_guardado")).isZero();
    }

    @Test
    void siLaGeneracionFallaLaActividadGuardadaNoSeDeshaceNiSePropagaElError() throws InterruptedException {
        doThrow(new IllegalStateException("falló la generación")).when(CALENDARIO).procesar();

        assertThatCode(() -> cambios.guardarActividad()).doesNotThrowAnyException();

        verify(CALENDARIO, timeout(5000)).procesar();
        assertThat(contar("plan_guardado")).isEqualTo(1);
    }

    @Test
    void variosCambiosSeguidosSeGeneranUnoTrasOtroSinSolaparse() throws InterruptedException {
        AtomicBoolean solapadas = new AtomicBoolean();
        AtomicBoolean enCurso = new AtomicBoolean();
        doAnswer(inv -> {
            if (!enCurso.compareAndSet(false, true)) solapadas.set(true);
            Thread.sleep(50);
            enCurso.set(false);
            return 0;
        }).when(CALENDARIO).procesar();

        cambios.guardarActividad();
        cambios.guardarActividad();
        cambios.guardarActividad();

        verify(CALENDARIO, timeout(5000).times(3)).procesar();
        assertThat(solapadas).isFalse();
    }

    private int contar(String tabla) {
        return otraConexion.sql("select count(*) from " + tabla).query(Integer.class).single();
    }

    private static void esperarA(java.util.function.BooleanSupplier condicion) {
        long limite = System.currentTimeMillis() + 5000;
        while (!condicion.getAsBoolean() && System.currentTimeMillis() < limite) {
            try { Thread.sleep(25); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
        }
    }

    @Configuration
    @EnableTransactionManagement
    static class Config {
        @Bean(destroyMethod = "close") DataSource dataSource() {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(System.getProperty("prueba.db.url"));
            ds.setMaximumPoolSize(1);            // igual que application-local.yml
            ds.setMinimumIdle(1);
            ds.setConnectionTimeout(3000);       // un bloqueo por falta de conexión se nota como fallo, no como espera de 30 s
            return ds;
        }
        @Bean PlatformTransactionManager transactionManager(DataSource ds) { return new DataSourceTransactionManager(ds); }
        @Bean JdbcClient jdbcClient(DataSource ds) { return JdbcClient.create(ds); }
        @Bean CalendarioSanitarioService calendario() { return CALENDARIO; }
        @Bean RegenerarCalendarioAlCambiarPlan regenerar(CalendarioSanitarioService calendario) {
            return new RegenerarCalendarioAlCambiarPlan(calendario);
        }
        @Bean Cambios cambios(JdbcClient jdbc, ApplicationEventPublisher publicador) { return new Cambios(jdbc, publicador); }
    }

    /** Hace de {@code PlanSanitarioService}: guarda algo y publica el evento dentro de la transacción. */
    static class Cambios {
        private final JdbcClient jdbc;
        private final ApplicationEventPublisher publicador;

        Cambios(JdbcClient jdbc, ApplicationEventPublisher publicador) {
            this.jdbc = jdbc;
            this.publicador = publicador;
        }

        @Transactional
        public void guardarActividad() throws InterruptedException {
            jdbc.sql("insert into plan_guardado(dato) values ('actividad')").update();
            publicador.publishEvent(new PlanSanitarioModificado());
            // Una operación que todavía tarda antes de confirmar: un listener que corriera antes del commit
            // (o que no esperara a él) vería la actividad sin confirmar.
            Thread.sleep(200);
        }

        @Transactional
        public void guardarYFallar() {
            jdbc.sql("insert into plan_guardado(dato) values ('actividad')").update();
            publicador.publishEvent(new PlanSanitarioModificado());
            throw new IllegalStateException("se revierte");
        }
    }
}
