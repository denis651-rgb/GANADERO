package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.PreferenciasNotificacion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PreferenciasNotificacionTest {

    @Test
    void tratamientoDesactivadoBloqueaPushSinCambiarLaCategoriaDeLaAlerta() {
        PreferenciasNotificacion preferencias = new PreferenciasNotificacion(
                UUID.randomUUID(), UUID.randomUUID(),
                true, true, false, false, true, true, true,
                true, true, true, true);

        assertThat(preferencias.permite(TipoAlerta.TRATAMIENTO_ATRASADO)).isFalse();
        assertThat(TipoAlerta.TRATAMIENTO_ATRASADO.categoria()).isEqualTo(CategoriaAlerta.TRATAMIENTO);
        assertThat(preferencias.permite(TipoAlerta.PARTO_PROXIMO)).isTrue();
        assertThat(preferencias.permite(TipoAlerta.CASO_CLINICO_CRITICO)).isTrue();
    }

    @Test
    void casoCriticoExigeSanidadYCasosCriticos() {
        PreferenciasNotificacion sinCasosCriticos = new PreferenciasNotificacion(
                UUID.randomUUID(), UUID.randomUUID(),
                true, true, true, true, true, true, true,
                false, true, true, true);

        assertThat(sinCasosCriticos.permite(TipoAlerta.CASO_CLINICO_CRITICO)).isFalse();
        assertThat(sinCasosCriticos.permite(TipoAlerta.VACUNA_PROXIMA)).isTrue();
    }
}
