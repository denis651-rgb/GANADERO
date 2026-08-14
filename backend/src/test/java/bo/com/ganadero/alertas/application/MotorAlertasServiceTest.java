package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
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
