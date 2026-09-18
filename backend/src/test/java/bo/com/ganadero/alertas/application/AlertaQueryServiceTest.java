package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.application.AlertaQueryService.PendientesPorTipo;
import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Alertas por atender agrupadas por tipo: lo que lee el dashboard en «Atención requerida». */
class AlertaQueryServiceTest {
    private final AlertaRepository repo = mock(AlertaRepository.class);
    private final AnimalRepository animales = mock(AnimalRepository.class);
    private final UUID empresa = UUID.randomUUID();

    private AlertaQueryService service(CurrentUser user) {
        return new AlertaQueryService(repo, animales, new UserContext(() -> user));
    }

    private CurrentUser propietario() {
        return new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of("PROPIETARIO"),
                Set.of("ALERTA_VER"), Set.of(), true);
    }

    @Test
    void agrupaPorTipoLoPendienteEnviadoOConErrorYTomaLaMayorSeveridad() {
        when(repo.listar(empresa, EstadoAlerta.PENDIENTE, null, null)).thenReturn(List.of(
                alerta(TipoAlerta.PARTO_PROXIMO, SeveridadAlerta.WARNING, EstadoAlerta.PENDIENTE, null),
                alerta(TipoAlerta.PARTO_PROXIMO, SeveridadAlerta.URGENTE, EstadoAlerta.PENDIENTE, null)));
        when(repo.listar(empresa, EstadoAlerta.ENVIADA, null, null)).thenReturn(List.of(
                alerta(TipoAlerta.VACUNA_VENCIDA, SeveridadAlerta.URGENTE, EstadoAlerta.ENVIADA, null)));
        when(repo.listar(empresa, EstadoAlerta.ERROR, null, null)).thenReturn(List.of(
                alerta(TipoAlerta.PARTO_PROXIMO, SeveridadAlerta.INFO, EstadoAlerta.ERROR, null)));

        List<PendientesPorTipo> resultado = service(propietario()).pendientesPorTipo();

        assertThat(resultado).containsExactlyInAnyOrder(
                new PendientesPorTipo(TipoAlerta.PARTO_PROXIMO, 3, NivelAtencion.URGENTE),
                new PendientesPorTipo(TipoAlerta.VACUNA_VENCIDA, 1, NivelAtencion.URGENTE));
    }

    @Test
    void noCuentaLoProgramadoAFuturoNiLoYaAtendidoResueltoOCancelado() {
        service(propietario()).pendientesPorTipo();

        for (EstadoAlerta noPendiente : List.of(EstadoAlerta.PROGRAMADA, EstadoAlerta.ATENDIDA,
                EstadoAlerta.RESUELTA, EstadoAlerta.CANCELADA)) {
            verify(repo, never()).listar(eq(empresa), eq(noPendiente), any(), any());
        }
    }

    @Test
    void laSeveridadInternaSeTraduceATresNivelesDeAtencion() {
        assertThat(NivelAtencion.de(SeveridadAlerta.CRITICA)).isEqualTo(NivelAtencion.URGENTE);
        assertThat(NivelAtencion.de(SeveridadAlerta.URGENTE)).isEqualTo(NivelAtencion.URGENTE);
        assertThat(NivelAtencion.de(SeveridadAlerta.WARNING)).isEqualTo(NivelAtencion.ADVERTENCIA);
        assertThat(NivelAtencion.de(SeveridadAlerta.INFO)).isEqualTo(NivelAtencion.INFORMATIVO);
    }

    @Test
    void sinAlertasPendientesDevuelveUnaListaVacia() {
        assertThat(service(propietario()).pendientesPorTipo()).isEmpty();
    }

    @Test
    void respetaLaVisibilidadPorPropiedadDeQuienConsulta() {
        UUID propia = UUID.randomUUID();
        UUID ajena = UUID.randomUUID();
        CurrentUser trabajador = new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of("TRABAJADOR"),
                Set.of("ALERTA_VER"), Set.of(propia), false);
        when(repo.listar(empresa, EstadoAlerta.PENDIENTE, null, null)).thenReturn(List.of(
                alerta(TipoAlerta.INVENTARIO_BAJO, SeveridadAlerta.WARNING, EstadoAlerta.PENDIENTE, Map.of("propiedadId", propia.toString())),
                alerta(TipoAlerta.INVENTARIO_BAJO, SeveridadAlerta.URGENTE, EstadoAlerta.PENDIENTE, Map.of("propiedadId", ajena.toString())),
                alerta(TipoAlerta.SISTEMA_REQUIERE_ATENCION, SeveridadAlerta.INFO, EstadoAlerta.PENDIENTE, null)));

        List<PendientesPorTipo> resultado = service(trabajador).pendientesPorTipo();

        // la alerta de la propiedad ajena no cuenta ni sube el nivel del grupo
        assertThat(resultado).containsExactlyInAnyOrder(
                new PendientesPorTipo(TipoAlerta.INVENTARIO_BAJO, 1, NivelAtencion.ADVERTENCIA),
                new PendientesPorTipo(TipoAlerta.SISTEMA_REQUIERE_ATENCION, 1, NivelAtencion.INFORMATIVO));
    }

    private Alerta alerta(TipoAlerta tipo, SeveridadAlerta severidad, EstadoAlerta estado, Map<String, Object> metadata) {
        return new Alerta(UUID.randomUUID(), empresa, null, tipo, "titulo", "mensaje", severidad, Instant.now(), null,
                "ORIGEN", UUID.randomUUID(), estado, metadata, null, null, null, null, null, null, null, 0, null, null,
                null, "clave-" + UUID.randomUUID());
    }
}
