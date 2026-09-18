package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MotorAlertasServiceTest {
    @Test
    void creaPartoProximoConElNombreYFechaDelAnimal() {
        AlertaRepository repository = mock(AlertaRepository.class);
        when(repository.programar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MotorAlertasService service = new MotorAlertasService(repository);
        Instant parto = Instant.parse("2026-08-30T04:00:00Z");

        service.programar(new ProgramarAlertaCommand(UUID.randomUUID(), UUID.randomUUID(),
                TipoAlerta.PARTO_PROXIMO, parto.minusSeconds(15L * 86400), parto,
                "GESTACION", UUID.randomUUID(), Map.of("animalNombre", "Lucera")));

        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(repository).programar(captor.capture());
        assertThat(captor.getValue().mensaje()).contains("Lucera", "30/08/2026");
        assertThat(captor.getValue().severidad()).isEqualTo(SeveridadAlerta.WARNING);
    }

    @Test
    void escalaUnaMismaVacunaSegunLosDiasRestantes() {
        assertThat(alertaVacuna(-7).severidad()).isEqualTo(SeveridadAlerta.CRITICA);
        assertThat(alertaVacuna(0).severidad()).isEqualTo(SeveridadAlerta.URGENTE);
        assertThat(alertaVacuna(3).severidad()).isEqualTo(SeveridadAlerta.WARNING);
        assertThat(alertaVacuna(7).severidad()).isEqualTo(SeveridadAlerta.INFO);
    }

    @Test
    void agrupaLaActividadSanitariaDeVariosAnimalesEnUnaSolaAlerta() {
        Alerta alerta = alertaActividad(10, 60);

        assertThat(alerta.animalId()).isNull();
        assertThat(alerta.titulo()).isEqualTo("Fiebre Aftosa próxima");
        assertThat(alerta.mensaje()).contains("Fiebre Aftosa", "60 animales previstos");
    }

    @Test
    void escalaLaSeveridadDeUnaActividadSegunSuCercania() {
        assertThat(alertaActividad(-8, 60).severidad()).isEqualTo(SeveridadAlerta.CRITICA);
        assertThat(alertaActividad(-1, 60).severidad()).isEqualTo(SeveridadAlerta.URGENTE);
        assertThat(alertaActividad(2, 60).severidad()).isEqualTo(SeveridadAlerta.WARNING);
        assertThat(alertaActividad(10, 60).severidad()).isEqualTo(SeveridadAlerta.INFO);
    }

    @Test
    void tratamientoAtrasadoIdentificaAlAnimal() {
        Alerta alerta = alerta(TipoAlerta.TRATAMIENTO_ATRASADO,
                Map.of("animalCodigo", "H-0025"));
        assertThat(alerta.mensaje()).isEqualTo("El tratamiento de H-0025 está atrasado.");
        assertThat(alerta.severidad()).isEqualTo(SeveridadAlerta.URGENTE);
    }

    @Test
    void generaLaMismaClaveParaElMismoEventoDePesaje() {
        UUID empresa = UUID.randomUUID();
        UUID animal = UUID.randomUUID();
        Alerta primera = alerta(empresa, animal, TipoAlerta.PESAJE_ATRASADO,
                Map.of("animalCodigo", "H-0005", "diasSinPesaje", 44,
                        "eventoReferencia", "2026-07-01"));
        Alerta segunda = alerta(empresa, animal, TipoAlerta.PESAJE_ATRASADO,
                Map.of("animalCodigo", "H-0005", "diasSinPesaje", 45,
                        "eventoReferencia", "2026-07-01"));

        assertThat(segunda.claveIdempotencia()).isEqualTo(primera.claveIdempotencia());
        assertThat(primera.mensaje()).isEqualTo("H-0005 lleva 44 días sin pesaje.");
    }

    @Test
    void unAvisoParaUnDiaSaleALaHoraDeAvisosPredeterminadaYNoAMedianoche() {
        Alerta alerta = alertaAlDia(null, LocalDate.of(2026, 8, 15));

        // 08:00 en La Paz (UTC-4) = 12:00Z, no 04:00Z (medianoche de La Paz).
        assertThat(alerta.fechaProgramada()).isEqualTo(Instant.parse("2026-08-15T12:00:00Z"));
    }

    @Test
    void unAvisoParaUnDiaRespetaLaHoraConfigurada() {
        Alerta alerta = alertaAlDia(new AlertaConfiguracion(15, 7, 30, 285, LocalTime.of(9, 30)),
                LocalDate.of(2026, 8, 15));

        assertThat(alerta.fechaProgramada()).isEqualTo(Instant.parse("2026-08-15T13:30:00Z"));
    }

    @Test
    void elDiaDelAvisoRespetaLosDiasDeAnticipacionYNoLosMuestraComoMetadata() {
        Alerta alerta = alertaAlDia(null, LocalDate.of(2026, 8, 15));

        assertThat(alerta.fechaVencimiento()).isEqualTo(Instant.parse("2026-08-30T04:00:00Z"));
        assertThat(alerta.mensaje()).contains("Lucera", "30/08/2026");
        assertThat(alerta.metadata()).doesNotContainKeys("diaDeAviso", "fechaVencimiento");
    }

    @Test
    void unAvisoParaUnDiaSinVencimientoNoInventaUno() {
        AlertaRepository repository = mock(AlertaRepository.class);
        when(repository.programar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        new MotorAlertasService(repository).programar(ProgramarAlertaCommand.alDia(UUID.randomUUID(),
                UUID.randomUUID(), TipoAlerta.RETIRO_CARNE_VIGENTE, LocalDate.of(2026, 8, 15), null,
                "RETIRO_CARNE", UUID.randomUUID(), Map.of("animalNombre", "Lucera", "tipoRetiro", "CARNE")));
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(repository).programar(captor.capture());

        assertThat(captor.getValue().fechaProgramada()).isEqualTo(Instant.parse("2026-08-15T12:00:00Z"));
        assertThat(captor.getValue().metadata()).doesNotContainKey("diaDeAviso");
    }

    @Test
    void unAvisoConHoraExplicitaNoCambiaDeHora() {
        // Los avisos que ya traen su hora (pesaje, actividades del calendario…) no pasan por la hora de avisos.
        Alerta alerta = alerta(TipoAlerta.PESAJE_ATRASADO, Map.of("animalCodigo", "H-1", "diasSinPesaje", 40));

        assertThat(alerta.fechaProgramada()).isEqualTo(Instant.parse("2026-08-20T04:00:00Z"));
    }

    private Alerta alertaAlDia(AlertaConfiguracion ajustes, LocalDate dia) {
        AlertaRepository repository = mock(AlertaRepository.class);
        when(repository.programar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AlertaConfiguracionPort puerto = mock(AlertaConfiguracionPort.class);
        when(puerto.obtener(any())).thenReturn(ajustes == null ? AlertaConfiguracion.valoresPredeterminados() : ajustes);
        new MotorAlertasService(repository, puerto).programar(ProgramarAlertaCommand.alDia(UUID.randomUUID(),
                UUID.randomUUID(), TipoAlerta.PARTO_PROXIMO, dia, Instant.parse("2026-08-30T04:00:00Z"),
                "GESTACION", UUID.randomUUID(), Map.of("animalNombre", "Lucera")));
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(repository).programar(captor.capture());
        return captor.getValue();
    }


    /** Actividad del plan agrupada: sin animalId y con la cantidad de animales en metadata. */
    private Alerta alertaActividad(int diasDesdeHoy, int cantidadAnimales) {
        AlertaRepository repository = mock(AlertaRepository.class);
        when(repository.programar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MotorAlertasService service = new MotorAlertasService(repository);
        Instant fecha = Instant.now().plus(diasDesdeHoy, ChronoUnit.DAYS);
        service.programar(new ProgramarAlertaCommand(UUID.randomUUID(), null, TipoAlerta.ACTIVIDAD_SANITARIA_PROXIMA,
                fecha, "EVENTO_CALENDARIO_SANITARIO", UUID.randomUUID(),
                Map.of("nombreActividad", "Fiebre Aftosa", "cantidadAnimales", cantidadAnimales)));
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(repository).programar(captor.capture());
        return captor.getValue();
    }

    private Alerta alertaVacuna(int diasRestantes) {
        TipoAlerta tipo = diasRestantes <= 0 ? TipoAlerta.VACUNA_VENCIDA : TipoAlerta.VACUNA_PROXIMA;
        return alerta(tipo, Map.of("animalCodigo", "ANI-001", "diasRestantes", diasRestantes));
    }

    private Alerta alerta(TipoAlerta tipo, Map<String, Object> metadata) {
        return alerta(UUID.randomUUID(), UUID.randomUUID(), tipo, metadata);
    }

    private Alerta alerta(UUID empresa, UUID animal, TipoAlerta tipo, Map<String, Object> metadata) {
        AlertaRepository repository = mock(AlertaRepository.class);
        when(repository.programar(any())).thenAnswer(invocation -> invocation.getArgument(0));
        MotorAlertasService service = new MotorAlertasService(repository);
        service.programar(new ProgramarAlertaCommand(empresa, animal, tipo,
                Instant.parse("2026-08-20T04:00:00Z"), "PRUEBA", animal, metadata));
        ArgumentCaptor<Alerta> captor = ArgumentCaptor.forClass(Alerta.class);
        verify(repository).programar(captor.capture());
        return captor.getValue();
    }
}
