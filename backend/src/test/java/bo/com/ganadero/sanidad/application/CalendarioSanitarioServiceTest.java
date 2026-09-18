package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.sanidad.infrastructure.JdbcSanidadRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.sqlite.SQLiteDataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Corre contra SQLite real: lo central a probar es que dos corridas del generador no dupliquen
 * eventos (el índice único de evento_calendario_sanitario hace el trabajo, pero sólo una prueba
 * de verdad contra la base lo demuestra) y que cada modalidad calcule la fecha prevista según su
 * propia regla (sección 20).
 */
class CalendarioSanitarioServiceTest {
    private static final UUID PROPIEDAD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void generaUnEventoPorEdadCuandoElAnimalEntraEnLaVentana(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        f.crearAnimal("N-001", LocalDate.now().minusDays(87), false);

        int generados = f.service.procesar();

        assertThat(generados).isEqualTo(1);
        assertThat(f.contarEventos()).isEqualTo(1);
    }

    @Test
    void noGeneraEventoPorEdadFueraDelHorizonteDeProyeccion(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        // La fecha objetivo queda después del horizonte predeterminado de 12 meses.
        f.crearAnimal("N-002", LocalDate.now().plusMonths(13).minusDays(90), false);

        int generados = f.service.procesar();

        assertThat(generados).isZero();
    }

    @Test
    void noDuplicaElEventoPorEdadAlCorrerElGeneradorDosVeces(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        f.crearAnimal("N-003", LocalDate.now().minusDays(89), false);

        f.service.procesar();
        f.service.procesar();

        assertThat(f.contarEventos()).isEqualTo(1);
    }

    @Test
    void excluyeAnimalesConEdadDesconocidaDeLaModalidadPorEdad(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPorEdad(90, 5, 15);
        f.crearAnimalSinFechaNacimiento("COMPRADO-01");

        int generados = f.service.procesar();

        assertThat(generados).isZero();
        assertThat(f.contarEventos()).isZero();
    }

    @Test
    void proyectaLosCiclosPeriodicosDentroDelHorizonteDesdeLaUltimaAplicacion(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID itemId = f.crearActividadPeriodica(90, 5, 0);
        UUID animalId = f.crearAnimal("N-004", LocalDate.now().minusYears(2), false);
        f.confirmarAplicacion(animalId, itemId, LocalDate.now().minusDays(88));

        int generados = f.service.procesar();

        assertThat(generados).isGreaterThan(1);
        assertThat(f.contarEventos()).isEqualTo(generados);
    }

    @Test
    void sinAntecedentePeriodicaUsaLaVigenciaDeLaActividadComoSemilla(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        // vigenteDesde queda en Instant.now() al crear el item; con frecuencia 0 el ciclo cae hoy mismo.
        f.crearActividadPeriodica(0, 5, 0);
        f.crearAnimal("N-005", LocalDate.now().minusYears(1), false);

        int generados = f.service.procesar();

        assertThat(generados).isEqualTo(1);
    }

    @Test
    void agrupaEnUnaSolaAlertaLosAnimalesQueCaenEnLaMismaOcurrencia(@TempDir Path tempDir) {
        MotorAlertas motor = mock(MotorAlertas.class);
        Fixture f = fixture(tempDir, motor);
        f.crearActividadPorEdad(90, 5, 15);
        LocalDate nacimiento = LocalDate.now().minusDays(87);
        f.crearAnimal("N-010", nacimiento, false);
        f.crearAnimal("N-011", nacimiento, false);
        f.crearAnimal("N-012", nacimiento, false);

        f.service.procesar();

        ArgumentCaptor<ProgramarAlertaCommand> captor = ArgumentCaptor.forClass(ProgramarAlertaCommand.class);
        verify(motor, times(1)).evolucionar(captor.capture(), any());
        assertThat(captor.getValue().animalId()).isNull();
        assertThat(captor.getValue().metadata()).containsEntry("cantidadAnimales", 3)
                .containsEntry("nombreActividad", "Desparasitación");
    }

    @Test
    void unaVezEnLaVidaNoReprogramaAQuienYaLaRecibioEnUnaVersionAnterior(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID v1 = f.crearActividadPorEdad(90, 5, 15, true);
        UUID yaAplicado = f.crearAnimal("N-020", LocalDate.now().minusDays(89), false);
        UUID pendiente = f.crearAnimal("N-021", LocalDate.now().minusDays(89), false);
        f.confirmarAplicacion(yaAplicado, v1, LocalDate.now().minusDays(1));
        UUID v2 = f.crearNuevaVersionPorEdad(v1, true);

        f.service.procesar();

        assertThat(f.animalesConEvento(v2)).containsExactly(pendiente);
    }

    @Test
    void unaVezEnLaVidaTambienRespetaLoDeclaradoPorElProveedor(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID v1 = f.crearActividadPorEdad(90, 5, 15, true);
        UUID declarado = f.crearAnimal("N-022", LocalDate.now().minusDays(89), false);
        UUID pendiente = f.crearAnimal("N-023", LocalDate.now().minusDays(89), false);
        f.confirmarAplicacion(declarado, v1, LocalDate.now().minusDays(30), "APLICADO", "DECLARADA_PROVEEDOR");
        UUID v2 = f.crearNuevaVersionPorEdad(v1, true);

        f.service.procesar();

        assertThat(f.animalesConEvento(v2)).containsExactly(pendiente);
    }

    @Test
    void sinUnaVezEnLaVidaProgramaDeTodosModosAQuienYaLaRecibio(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID v1 = f.crearActividadPorEdad(90, 5, 15, false);
        UUID yaAplicado = f.crearAnimal("N-024", LocalDate.now().minusDays(89), false);
        UUID pendiente = f.crearAnimal("N-025", LocalDate.now().minusDays(89), false);
        f.confirmarAplicacion(yaAplicado, v1, LocalDate.now().minusDays(1));
        UUID v2 = f.crearNuevaVersionPorEdad(v1, false);

        f.service.procesar();

        assertThat(f.animalesConEvento(v2)).containsExactlyInAnyOrder(yaAplicado, pendiente);
    }

    @Test
    void unaAplicacionAnuladaNoCuentaComoRecibida(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID v1 = f.crearActividadPorEdad(90, 5, 15, true);
        UUID anulada = f.crearAnimal("N-026", LocalDate.now().minusDays(89), false);
        f.confirmarAplicacion(anulada, v1, LocalDate.now().minusDays(1), "ANULADO", "APLICADA_FINCA");
        UUID v2 = f.crearNuevaVersionPorEdad(v1, true);

        f.service.procesar();

        assertThat(f.animalesConEvento(v2)).containsExactly(anulada);
    }

    @Test
    void loAplicadoDeOtraActividadNoImpideProgramarEstaUnaVezEnLaVida(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPorEdad(90, 5, 15, true);
        UUID otraActividad = f.crearActividadPorEdad(90, 5, 15, true);
        UUID animal = f.crearAnimal("N-027", LocalDate.now().minusDays(89), false);
        f.confirmarAplicacion(animal, otraActividad, LocalDate.now().minusDays(1));

        f.service.procesar();

        assertThat(f.animalesConEvento(actividad)).containsExactly(animal);
    }

    @Test
    void noGeneraEventosRetroactivosParaAnimalesCuyaVentanaYaSeCerroAntesDeLaActividad(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPorEdad(210, 15, 30, true);
        f.crearAnimal("N-030-ADULTO", LocalDate.now().minusDays(1000), false);
        UUID enVentana = f.crearAnimal("N-031-VENTANA", LocalDate.now().minusDays(200), false);

        f.service.procesar();
        f.service.procesar();

        assertThat(f.animalesConEvento(actividad)).containsExactly(enVentana);
        assertThat(f.contarEventosVencidos()).isZero();
    }

    @Test
    void siLaVentanaSeCerroEstandoLaActividadVigenteQuedaVencidaComoIncumplimientoReal(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPorEdad(210, 15, 30, true);
        f.definirVigenciaDesde(actividad, Instant.now().minus(java.time.Duration.ofDays(100)));
        UUID animal = f.crearAnimal("N-032", LocalDate.now().minusDays(250), false);

        f.service.procesar();
        f.service.procesar();

        assertThat(f.animalesConEvento(actividad)).containsExactly(animal);
        assertThat(f.contarEventosVencidos()).isEqualTo(1);
    }

    @Test
    void noGeneraCiclosPeriodicosPasadosCuandoLaReferenciaEsElNacimiento(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPeriodica(90, 0, 0, ReferenciaCalculoPeriodica.FECHA_DE_NACIMIENTO, null, null, false);
        // Ciclos a los 90/180/270/360 días ya pasaron; quedan 450/540/630/720 dentro del horizonte de 12 meses.
        f.crearAnimal("N-033", LocalDate.now().minusDays(400), false);

        f.service.procesar();

        assertThat(f.contarEventos()).isEqualTo(4);
        assertThat(f.contarEventosVencidos()).isZero();
    }

    @Test
    void laActividadPeriodicaDejaDeProgramarCuandoElAnimalSuperaLaEdadMaxima(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPeriodica(30, 0, 0, ReferenciaCalculoPeriodica.ULTIMA_APLICACION, null, 150, false);
        // 100 días hoy: el primer ciclo lo agarra con 130 días, el segundo ya con 160 (> 150).
        f.crearAnimal("N-034", LocalDate.now().minusDays(100), false);

        f.service.procesar();

        assertThat(f.contarEventos()).isEqualTo(1);
    }

    @Test
    void conRangoDeEdadExcluyeALosDeEdadDesconocida(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPeriodica(30, 0, 0, ReferenciaCalculoPeriodica.ULTIMA_APLICACION, null, 150, false);
        f.crearAnimalSinFechaNacimiento("N-035");

        f.service.procesar();

        assertThat(f.contarEventos()).isZero();
    }

    @Test
    void conRangoDeEdadIncluyeALosDeEdadDesconocidaSiLaActividadLoPermite(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        f.crearActividadPeriodica(30, 0, 0, ReferenciaCalculoPeriodica.ULTIMA_APLICACION, null, 150, true);
        f.crearAnimalSinFechaNacimiento("N-036");

        f.service.procesar();

        assertThat(f.contarEventos()).isGreaterThan(0);
    }

    @Test
    void alCancelarLosPendientesDeUnaVersionElAnimalQuedaConUnSoloEventoPendiente(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID v1 = f.crearActividadPorEdad(90, 5, 15, false);
        UUID animalA = f.crearAnimal("N-040", LocalDate.now().minusDays(89), false);
        UUID animalB = f.crearAnimal("N-041", LocalDate.now().minusDays(89), false);
        f.service.procesar();
        f.marcarEvento(v1, animalB, "EN_PREPARACION"); // ya está en una jornada: no se debe cancelar
        UUID v2 = f.crearNuevaVersionPorEdad(v1, false);
        var eventos = new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(f.jdbc());

        List<UUID> ocurrencias = eventos.cancelarPendientesDeActividad(v1);
        f.service.procesar();

        assertThat(ocurrencias).hasSize(1);
        assertThat(f.estadoEvento(v1, animalA)).isEqualTo("CANCELADO");
        assertThat(f.estadoEvento(v1, animalB)).isEqualTo("EN_PREPARACION");
        assertThat(f.eventosPendientesDelAnimal(animalA)).isEqualTo(1); // solo el de la versión nueva
        assertThat(f.animalesConEvento(v2)).containsExactlyInAnyOrder(animalA, animalB);
    }

    @Test
    void cancelarPendientesNoTocaLoQueYaSeCerroNiLoVencido(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID v1 = f.crearActividadPorEdad(90, 5, 15, false);
        UUID realizado = f.crearAnimal("N-042", LocalDate.now().minusDays(89), false);
        UUID vencido = f.crearAnimal("N-043", LocalDate.now().minusDays(89), false);
        f.service.procesar();
        f.marcarEvento(v1, realizado, "REALIZADO");
        f.marcarEvento(v1, vencido, "VENCIDO");
        var eventos = new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(f.jdbc());

        List<UUID> ocurrencias = eventos.cancelarPendientesDeActividad(v1);

        assertThat(ocurrencias).isEmpty();
        assertThat(f.estadoEvento(v1, realizado)).isEqualTo("REALIZADO");
        assertThat(f.estadoEvento(v1, vencido)).isEqualTo("VENCIDO");
    }

    @Test
    void generaEventosParaTodoElHatoSinTopeDeAnimales(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPorEdad(90, 5, 15, true);
        LocalDate nacimiento = LocalDate.now().minusDays(89);
        for (int i = 0; i < 501; i++) f.crearAnimal("HATO-" + i, nacimiento, false);

        f.service.procesar();

        assertThat(f.animalesConEvento(actividad)).hasSize(501);
    }

    @Test
    void cancelarLosPendientesDeUnPlanNoTocaLosDeOtroPlan(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividadA = f.crearActividadPorEdad(90, 5, 15, true);
        UUID actividadB = f.crearActividadPorEdad(90, 5, 15, true);
        UUID animal = f.crearAnimal("N-050", LocalDate.now().minusDays(89), false);
        f.service.procesar();
        var eventos = new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(f.jdbc());

        List<UUID> ocurrencias = eventos.cancelarPendientesDePlan(f.planDe(actividadA));

        assertThat(ocurrencias).hasSize(1);
        assertThat(f.estadoEvento(actividadA, animal)).isEqualTo("CANCELADO");
        assertThat(f.estadoEvento(actividadB, animal)).isEqualTo("PROGRAMADO");
    }

    @Test
    void reactivarUnaActividadRestauraLoCanceladoFuturoYNoResucitaLoVencido(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPorEdad(90, 5, 15, true);
        UUID futuro = f.crearAnimal("N-051", LocalDate.now().minusDays(89), false);
        UUID pasado = f.crearAnimal("N-052", LocalDate.now().minusDays(89), false);
        f.service.procesar();
        var eventos = new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(f.jdbc());
        eventos.cancelarPendientesDeActividad(actividad);
        f.moverEventoAlPasado(actividad, pasado);

        eventos.restaurarCanceladosFuturos(actividad);

        assertThat(f.estadoEvento(actividad, futuro)).isEqualTo("PROYECTADO");
        assertThat(f.estadoEvento(actividad, pasado)).isEqualTo("CANCELADO");
    }

    @Test
    void desactivarYReactivarUnaActividadDejaAlAnimalConUnSoloEventoVigente(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPorEdad(90, 5, 15, true);
        UUID animal = f.crearAnimal("N-053", LocalDate.now().minusDays(89), false);
        f.service.procesar();
        var eventos = new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(f.jdbc());

        eventos.cancelarPendientesDeActividad(actividad); // desactivar
        f.service.procesar();                               // desactivada: el generador no la procesa
        assertThat(f.estadoEvento(actividad, animal)).isEqualTo("CANCELADO");

        eventos.restaurarCanceladosFuturos(actividad);      // reactivar
        f.service.procesar();

        assertThat(f.animalesConEvento(actividad)).containsExactly(animal);
        assertThat(f.estadoEvento(actividad, animal)).isEqualTo("PROGRAMADO");
    }

    // ---------- serie periódica desde «Última aplicación»: conciliación ----------

    private static final String[] PENDIENTES = {"PROYECTADO", "PROGRAMADO"};

    private static java.time.LocalDate hoyLaPaz() {
        return java.time.LocalDate.now(java.time.ZoneId.of("America/La_Paz"));
    }

    @Test
    void alRegistrarUnaAplicacionFueraDeFechaLaSerieViejaSeReemplazaSinDuplicados(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPeriodica(21, 0, 0); // «Última aplicación»
        UUID animal = f.crearAnimal("P-001", LocalDate.now().minusYears(2), false);
        f.service.procesar();
        List<LocalDate> serieVieja = f.fechas(actividad, animal, PENDIENTES);
        assertThat(serieVieja).hasSize(17);

        LocalDate aplicacion = hoyLaPaz().minusDays(3);
        f.confirmarAplicacion(animal, actividad, aplicacion);
        f.service.procesar();

        List<LocalDate> vigentes = f.fechas(actividad, animal, PENDIENTES);
        assertThat(vigentes).hasSize(17).doesNotHaveDuplicates();
        assertThat(vigentes).allSatisfy(d ->
                assertThat(java.time.temporal.ChronoUnit.DAYS.between(aplicacion, d) % 21).isZero());
        assertThat(f.fechas(actividad, animal, "CANCELADO")).containsExactlyElementsOf(serieVieja);
        // otra corrida más no cambia nada
        f.service.procesar();
        assertThat(f.fechas(actividad, animal, PENDIENTES)).containsExactlyElementsOf(vigentes);
        assertThat(f.fechas(actividad, animal, "CANCELADO")).hasSize(17);
    }

    @Test
    void siLaSerieVuelveAAlinearseRestauraLasFechasQueSeHabianCancelado(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPeriodica(21, 0, 0);
        UUID animal = f.crearAnimal("P-002", LocalDate.now().minusYears(2), false);
        f.service.procesar();
        List<LocalDate> serieInicial = f.fechas(actividad, animal, PENDIENTES);
        f.confirmarAplicacion(animal, actividad, hoyLaPaz().minusDays(3));
        f.service.procesar();
        // una segunda aplicación el día de la vigencia devuelve la referencia al punto de partida original
        f.confirmarAplicacion(animal, actividad, hoyLaPaz());

        f.service.procesar();

        assertThat(f.fechas(actividad, animal, PENDIENTES)).containsExactlyElementsOf(serieInicial);
        assertThat(f.fechas(actividad, animal, "CANCELADO")).hasSize(17).doesNotContainAnyElementsOf(serieInicial);
    }

    @Test
    void alConciliarResuelveLasAlertasDeLasOcurrenciasQueQuedaronSinPendientes(@TempDir Path tempDir) {
        MotorAlertas motor = mock(MotorAlertas.class);
        Fixture f = fixture(tempDir, motor);
        UUID actividad = f.crearActividadPeriodica(21, 0, 0);
        UUID animal = f.crearAnimal("P-003", LocalDate.now().minusYears(2), false);
        f.service.procesar();
        f.confirmarAplicacion(animal, actividad, hoyLaPaz().minusDays(3));

        f.service.procesar();

        verify(motor, org.mockito.Mockito.atLeastOnce())
                .resolverPorOrigen(any(), org.mockito.ArgumentMatchers.eq("EVENTO_CALENDARIO_SANITARIO"), any());
    }

    @Test
    void siLaOcurrenciaAunTieneAOtroAnimalNoSeResuelveSuAlertaNiSeTocaSuSerie(@TempDir Path tempDir) {
        MotorAlertas motor = mock(MotorAlertas.class);
        Fixture f = fixture(tempDir, motor);
        UUID actividad = f.crearActividadPeriodica(21, 0, 0);
        UUID aplicado = f.crearAnimal("P-004", LocalDate.now().minusYears(2), false);
        UUID otro = f.crearAnimal("P-005", LocalDate.now().minusYears(2), false);
        f.service.procesar();
        List<LocalDate> serieDelOtro = f.fechas(actividad, otro, PENDIENTES);
        f.confirmarAplicacion(aplicado, actividad, hoyLaPaz().minusDays(3));

        f.service.procesar();

        assertThat(f.fechas(actividad, otro, PENDIENTES)).containsExactlyElementsOf(serieDelOtro);
        assertThat(f.fechas(actividad, otro, "CANCELADO")).isEmpty();
        assertThat(f.fechas(actividad, aplicado, "CANCELADO")).hasSize(17);
        // sus ocurrencias viejas todavía tienen a «otro»: no hay alerta que resolver
        verify(motor, org.mockito.Mockito.never()).resolverPorOrigen(any(), any(), any());
    }

    @Test
    void laConciliacionNoTocaLoQueYaEstaEnUnaJornada(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPeriodica(21, 0, 0);
        UUID animal = f.crearAnimal("P-006", LocalDate.now().minusYears(2), false);
        f.service.procesar();
        f.marcarEvento(actividad, animal, "EN_PREPARACION");
        f.confirmarAplicacion(animal, actividad, hoyLaPaz().minusDays(3));

        f.service.procesar();

        assertThat(f.fechas(actividad, animal, "CANCELADO")).isEmpty();
        assertThat(f.fechas(actividad, animal, "EN_PREPARACION")).hasSize(17);
    }

    @Test
    void conUnaReferenciaFijaRegistrarUnaAplicacionNoCancelaNada(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPeriodica(90, 0, 0, ReferenciaCalculoPeriodica.FECHA_DE_NACIMIENTO, null, null, false);
        UUID animal = f.crearAnimal("P-007", LocalDate.now().minusDays(400), false);
        f.service.procesar();
        List<LocalDate> antes = f.fechas(actividad, animal, PENDIENTES);
        f.confirmarAplicacion(animal, actividad, hoyLaPaz().minusDays(3));

        f.service.procesar();

        assertThat(f.fechas(actividad, animal, PENDIENTES)).containsExactlyElementsOf(antes);
        assertThat(f.fechas(actividad, animal, "CANCELADO")).isEmpty();
    }

    @Test
    void laUltimaAplicacionCuentaLasHechasEnVersionesAnteriores(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID v1 = f.crearActividadPeriodica(21, 0, 0);
        UUID animal = f.crearAnimal("P-008", LocalDate.now().minusYears(2), false);
        LocalDate aplicacion = hoyLaPaz().minusDays(10);
        f.confirmarAplicacion(animal, v1, aplicacion);
        UUID v2 = f.crearNuevaVersionPeriodica(v1, 21);

        f.service.procesar();

        // cuenta desde la aplicación hecha con la v1, no desde el día en que nació la v2
        assertThat(f.fechas(v2, animal, PENDIENTES)).first().isEqualTo(aplicacion.plusDays(21));
    }

    // ---------- aplicar tarde: qué eventos puede cerrar una aplicación ----------

    @Test
    void unaAplicacionPuedeCerrarLosPendientesYLosVencidosPeroNoLosYaCerrados(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPeriodica(21, 0, 0);
        UUID animal = f.crearAnimal("V-001", LocalDate.now().minusYears(2), false);
        f.service.procesar();
        List<String> ids = f.jdbc().sql("select id from evento_calendario_sanitario where animal_id=:a order by fecha_prevista")
                .param("a", animal.toString()).query(String.class).list();
        String[] estados = {"VENCIDO", "REALIZADO", "CANCELADO", "OMITIDO"};
        for (int i = 0; i < estados.length; i++) {
            f.jdbc().sql("update evento_calendario_sanitario set estado=:e where id=:id")
                    .param("e", estados[i]).param("id", ids.get(i)).update();
        }
        var eventos = new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(f.jdbc());

        var cerrables = eventos.cerrablesPorAplicacion(actividad, animal);

        // 17 fechas: 1 vencida + 13 pendientes; realizada, cancelada y omitida quedan fuera
        assertThat(cerrables).hasSize(17 - 3);
        assertThat(cerrables).extracting(e -> e.estado().name()).contains("VENCIDO")
                .doesNotContain("REALIZADO", "CANCELADO", "OMITIDO");
        assertThat(cerrables).isSortedAccordingTo(java.util.Comparator.comparing(
                bo.com.ganadero.sanidad.domain.EventoCalendarioSanitario::fechaPrevista));
    }

    @Test
    void unaOcurrenciaConUnVencidoSigueSinCerrarPeroNoTienePendientes(@TempDir Path tempDir) {
        Fixture f = fixture(tempDir);
        UUID actividad = f.crearActividadPeriodica(21, 0, 0);
        UUID animal = f.crearAnimal("V-002", LocalDate.now().minusYears(2), false);
        f.service.procesar();
        var eventos = new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(f.jdbc());
        var primero = eventos.cerrablesPorAplicacion(actividad, animal).get(0);
        UUID ocurrencia = primero.ocurrenciaId();

        f.jdbc().sql("update evento_calendario_sanitario set estado='VENCIDO' where id=:id")
                .param("id", primero.id().toString()).update();
        assertThat(eventos.tienePendientes(ocurrencia)).isFalse();
        assertThat(eventos.tieneSinCerrar(ocurrencia)).isTrue();

        eventos.marcarEstado(primero.id(), bo.com.ganadero.sanidad.domain.EstadoEventoCalendario.REALIZADO, null, null);
        assertThat(eventos.tieneSinCerrar(ocurrencia)).isFalse();
    }

    private Fixture fixture(Path tempDir) {
        return fixture(tempDir, null);
    }

    private Fixture fixture(Path tempDir, MotorAlertas motor) {
        DataSource dataSource = sqliteDataSource(tempDir);
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").mixed(true).load().migrate();
        JdbcClient jdbc = JdbcClient.create(dataSource);
        JdbcSanidadRepository planes = new JdbcSanidadRepository(jdbc, new tools.jackson.databind.ObjectMapper());
        @SuppressWarnings("unchecked")
        ObjectProvider<MotorAlertas> alertas = mock(ObjectProvider.class);
        if (motor != null) when(alertas.getIfAvailable()).thenReturn(motor);
        CalendarioSanitarioService service = new CalendarioSanitarioService(planes, jdbc,
                new bo.com.ganadero.sanidad.infrastructure.JdbcEventoCalendarioSanitarioRepository(jdbc),
                new bo.com.ganadero.sanidad.infrastructure.JdbcOcurrenciaCalendarioSanitarioRepository(jdbc),
                alertas);
        return new Fixture(jdbc, planes, service);
    }

    private DataSource sqliteDataSource(Path tempDir) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + tempDir.resolve("calendario-test.db") + "?foreign_keys=on");
        return dataSource;
    }

    private record Fixture(JdbcClient jdbc, JdbcSanidadRepository planes, CalendarioSanitarioService service) {

        UUID crearActividadPorEdad(int edadObjetivoDias, int ventanaAnticipadaDias, int ventanaPosteriorDias) {
            return crearActividadPorEdad(edadObjetivoDias, ventanaAnticipadaDias, ventanaPosteriorDias, true);
        }

        UUID crearActividadPorEdad(int edadObjetivoDias, int ventanaAnticipadaDias, int ventanaPosteriorDias,
                                   boolean unaVezEnLaVida) {
            UUID actor = UUID.randomUUID();
            PlanSanitario plan = planes.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan 2026", null,
                    LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0, null), actor);
            PlanSanitarioItem item = construir(plan.id(), ModalidadActividad.POR_EDAD,
                    configPorEdad(edadObjetivoDias, ventanaAnticipadaDias, ventanaPosteriorDias, unaVezEnLaVida));
            return planes.crearItem(item, actor).id();
        }

        /** Simula editar una actividad ya usada: cierra la vigencia de la versión anterior y crea otra fila con la misma identidad. */
        UUID crearNuevaVersionPorEdad(UUID versionAnterior, boolean unaVezEnLaVida) {
            UUID[] fila = jdbc.sql("select plan_id, identidad_logica_id from plan_sanitario_item where id=:id")
                    .param("id", versionAnterior.toString())
                    .query((r, n) -> new UUID[]{UUID.fromString(r.getString("plan_id")),
                            UUID.fromString(r.getString("identidad_logica_id"))}).single();
            jdbc.sql("update plan_sanitario_item set vigente_hasta=:hasta where id=:id")
                    .param("hasta", Instant.now().toString()).param("id", versionAnterior.toString()).update();
            UUID id = UUID.randomUUID();
            PlanSanitarioItem nueva = construir(id, fila[0], ModalidadActividad.POR_EDAD,
                    configPorEdad(90, 5, 15, unaVezEnLaVida), fila[1], 2, versionAnterior);
            return planes.crearItem(nueva, UUID.randomUUID()).id();
        }

        private ModalidadConfig.PorEdadConfig configPorEdad(int edadObjetivoDias, int ventanaAnticipadaDias,
                                                            int ventanaPosteriorDias, boolean unaVezEnLaVida) {
            return new ModalidadConfig.PorEdadConfig(edadObjetivoDias, UnidadEdadActividad.DIAS, ventanaAnticipadaDias,
                    ventanaPosteriorDias, PoliticaEdadEstimada.PERMITIR, PoliticaEdadDesconocida.EXCLUIR, unaVezEnLaVida);
        }

        UUID crearActividadPeriodica(int frecuenciaDias, int toleranciaAnticipada, int toleranciaPosterior) {
            return crearActividadPeriodica(frecuenciaDias, toleranciaAnticipada, toleranciaPosterior,
                    ReferenciaCalculoPeriodica.ULTIMA_APLICACION, null, null, false);
        }

        UUID crearActividadPeriodica(int frecuenciaDias, int toleranciaAnticipada, int toleranciaPosterior,
                                     ReferenciaCalculoPeriodica referencia, Integer edadMinDias, Integer edadMaxDias,
                                     boolean permiteEdadDesconocida) {
            UUID actor = UUID.randomUUID();
            PlanSanitario plan = planes.crearPlan(new PlanSanitario(UUID.randomUUID(), null, "Plan 2026", null,
                    LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0, null), actor);
            UUID id = UUID.randomUUID();
            PlanSanitarioItem item = construir(id, plan.id(), ModalidadActividad.PERIODICA,
                    new ModalidadConfig.PeriodicaConfig(frecuenciaDias, UnidadFrecuencia.DIAS, referencia,
                            toleranciaAnticipada, toleranciaPosterior),
                    id, 1, null, edadMinDias, edadMaxDias, permiteEdadDesconocida);
            return planes.crearItem(item, actor).id();
        }

        private PlanSanitarioItem construir(UUID planId, ModalidadActividad modalidad, ModalidadConfig config) {
            UUID id = UUID.randomUUID();
            return construir(id, planId, modalidad, config, id, 1, null);
        }

        private PlanSanitarioItem construir(UUID id, UUID planId, ModalidadActividad modalidad, ModalidadConfig config,
                                            UUID identidadLogicaId, int numeroVersion, UUID versionAnteriorId) {
            return construir(id, planId, modalidad, config, identidadLogicaId, numeroVersion, versionAnteriorId,
                    null, null, false);
        }

        private PlanSanitarioItem construir(UUID id, UUID planId, ModalidadActividad modalidad, ModalidadConfig config,
                                            UUID identidadLogicaId, int numeroVersion, UUID versionAnteriorId,
                                            Integer edadMinDias, Integer edadMaxDias, boolean permiteEdadDesconocida) {
            return new PlanSanitarioItem(id, null, planId, TipoActividadSanitaria.DESPARASITACION, null,
                    "Ivermectina 1%", null, null, edadMinDias, edadMaxDias, null, null, null, 0, null, false, true, 0,
                    OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO", permiteEdadDesconocida,
                    identidadLogicaId, numeroVersion, versionAnteriorId,
                    Instant.now(), null, null, null, "Desparasitación", null, "Ivermectina", null, null, null, null,
                    null, TipoCalculoDosis.NO_APLICA, null, null, null, null, null, null, null, List.of(),
                    UnidadEdadActividad.DIAS, modalidad, config, false);
        }

        UUID crearAnimal(String codigo, LocalDate fechaNacimiento, boolean estimada) {
            sembrarCatalogos();
            UUID animalId = UUID.randomUUID();
            jdbc.sql("""
                    insert into animal(id,codigo,sexo,fecha_nacimiento,fecha_nacimiento_estimada,raza_principal_id,
                        categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso)
                    values(:id,:cod,'HEMBRA',:fn,:est,'50000000-0000-0000-0000-000000000001',
                        '60000000-0000-0000-0000-000000000001','CARNE','NACIDO','40000000-0000-0000-0000-000000000001',:ingreso)
                    """).param("id", animalId.toString()).param("cod", codigo).param("fn", fechaNacimiento.toString())
                    .param("est", estimada).param("ingreso", fechaNacimiento.toString()).update();
            return animalId;
        }

        UUID crearAnimalSinFechaNacimiento(String codigo) {
            sembrarCatalogos();
            UUID animalId = UUID.randomUUID();
            jdbc.sql("""
                    insert into animal(id,codigo,sexo,raza_principal_id,categoria_actual_id,proposito,origen,
                        potrero_actual_id,fecha_ingreso)
                    values(:id,:cod,'HEMBRA','50000000-0000-0000-0000-000000000001',
                        '60000000-0000-0000-0000-000000000001','CARNE','COMPRADO','40000000-0000-0000-0000-000000000001',:ingreso)
                    """).param("id", animalId.toString()).param("cod", codigo).param("ingreso", LocalDate.now().toString()).update();
            return animalId;
        }

        private void sembrarCatalogos() {
            jdbc.sql("insert into raza(id,codigo,nombre) values('50000000-0000-0000-0000-000000000001','BRAHMAN','Brahman') on conflict do nothing").update();
            jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable) values('60000000-0000-0000-0000-000000000001','VAQUILLONA','Vaquillona','AMBOS') on conflict do nothing").update();
            jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id,activo) values('40000000-0000-0000-0000-000000000001','POT-01','Potrero',:p,1) on conflict do nothing")
                    .param("p", PROPIEDAD_ID.toString()).update();
        }

        void confirmarAplicacion(UUID animalId, UUID itemId, LocalDate fechaAplicacion) {
            confirmarAplicacion(animalId, itemId, fechaAplicacion, "APLICADO", "APLICADA_FINCA");
        }

        void confirmarAplicacion(UUID animalId, UUID itemId, LocalDate fechaAplicacion, String estado, String origenRegistro) {
            jdbc.sql("""
                    insert into aplicacion_sanitaria(id,plan_item_id,animal_id,fecha_aplicacion,idempotency_key,estado,origen_registro)
                    values(:id,:item,:animal,:fecha,:key,:estado,:origen)
                    """).param("id", UUID.randomUUID().toString()).param("item", itemId.toString())
                    .param("animal", animalId.toString()).param("fecha", fechaAplicacion.toString())
                    .param("key", UUID.randomUUID().toString()).param("estado", estado).param("origen", origenRegistro).update();
        }

        List<UUID> animalesConEvento(UUID actividadId) {
            return jdbc.sql("select animal_id from evento_calendario_sanitario where actividad_id=:a")
                    .param("a", actividadId.toString())
                    .query((r, n) -> UUID.fromString(r.getString("animal_id"))).list();
        }

        int contarEventos() {
            return jdbc.sql("select count(*) from evento_calendario_sanitario").query(Integer.class).single();
        }

        int contarEventosVencidos() {
            return jdbc.sql("select count(*) from evento_calendario_sanitario where estado='VENCIDO'")
                    .query(Integer.class).single();
        }

        void definirVigenciaDesde(UUID actividadId, Instant vigenteDesde) {
            jdbc.sql("update plan_sanitario_item set vigente_desde=:d where id=:id")
                    .param("d", vigenteDesde.toString()).param("id", actividadId.toString()).update();
        }

        void marcarEvento(UUID actividadId, UUID animalId, String estado) {
            jdbc.sql("update evento_calendario_sanitario set estado=:e where actividad_id=:a and animal_id=:an")
                    .param("e", estado).param("a", actividadId.toString()).param("an", animalId.toString()).update();
        }

        String estadoEvento(UUID actividadId, UUID animalId) {
            return jdbc.sql("select estado from evento_calendario_sanitario where actividad_id=:a and animal_id=:an")
                    .param("a", actividadId.toString()).param("an", animalId.toString()).query(String.class).single();
        }

        /** Fechas (día de La Paz) de los eventos de un animal y actividad en los estados dados, ordenadas. */
        List<LocalDate> fechas(UUID actividadId, UUID animalId, String... estados) {
            return jdbc.sql("""
                    select substr(fecha_prevista,1,10) from evento_calendario_sanitario
                    where actividad_id=:a and animal_id=:an and estado in (:estados) order by 1
                    """).param("a", actividadId.toString()).param("an", animalId.toString())
                    .param("estados", List.of(estados)).query(String.class).list().stream().map(LocalDate::parse).toList();
        }

        /** Simula editar una actividad periódica ya usada: cierra la versión anterior y crea otra con la misma identidad. */
        UUID crearNuevaVersionPeriodica(UUID versionAnterior, int frecuenciaDias) {
            UUID[] fila = jdbc.sql("select plan_id, identidad_logica_id from plan_sanitario_item where id=:id")
                    .param("id", versionAnterior.toString())
                    .query((r, n) -> new UUID[]{UUID.fromString(r.getString("plan_id")),
                            UUID.fromString(r.getString("identidad_logica_id"))}).single();
            jdbc.sql("update plan_sanitario_item set vigente_hasta=:hasta where id=:id")
                    .param("hasta", Instant.now().toString()).param("id", versionAnterior.toString()).update();
            UUID id = UUID.randomUUID();
            PlanSanitarioItem nueva = construir(id, fila[0], ModalidadActividad.PERIODICA,
                    new ModalidadConfig.PeriodicaConfig(frecuenciaDias, UnidadFrecuencia.DIAS,
                            ReferenciaCalculoPeriodica.ULTIMA_APLICACION, 0, 0), fila[1], 2, versionAnterior);
            return planes.crearItem(nueva, UUID.randomUUID()).id();
        }

        UUID planDe(UUID actividadId) {
            return jdbc.sql("select plan_id from plan_sanitario_item where id=:id").param("id", actividadId.toString())
                    .query((r, n) -> UUID.fromString(r.getString("plan_id"))).single();
        }

        void moverEventoAlPasado(UUID actividadId, UUID animalId) {
            jdbc.sql("update evento_calendario_sanitario set fecha_prevista=:f where actividad_id=:a and animal_id=:an")
                    .param("f", Instant.now().minus(java.time.Duration.ofDays(10)).toString())
                    .param("a", actividadId.toString()).param("an", animalId.toString()).update();
        }

        int eventosPendientesDelAnimal(UUID animalId) {
            return jdbc.sql("""
                    select count(*) from evento_calendario_sanitario
                    where animal_id=:an and estado in ('PROYECTADO','PROGRAMADO','EN_PREPARACION')
                    """).param("an", animalId.toString()).query(Integer.class).single();
        }
    }
}
