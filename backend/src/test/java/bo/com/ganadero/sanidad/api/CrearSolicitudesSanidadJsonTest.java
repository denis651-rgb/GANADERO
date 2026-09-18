package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.sanidad.application.CrearCasoClinicoCommand;
import bo.com.ganadero.sanidad.application.CrearTratamientoCommand;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class CrearSolicitudesSanidadJsonTest {
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

    @Test
    void casoClinicoAceptaFechaEnFormatoDelFormulario() {
        String body = "{\"animalId\":\"70000000-0000-0000-0000-000000000001\","
                + "\"fechaInicio\":\"2026-09-01\",\"sintomas\":\"Fiebre\",\"severidad\":\"LEVE\"}";

        CrearCasoClinicoCommand command = json.readValue(body, CrearCasoClinicoRequest.class).command();

        assertThat(command.fechaInicio()).isEqualTo(LocalDate.of(2026, 9, 1).atStartOfDay(BOLIVIA).toInstant());
    }

    @Test
    void tratamientoAceptaFechasEnFormatoDelFormulario() {
        String body = "{\"animalId\":\"70000000-0000-0000-0000-000000000001\","
                + "\"fechaInicio\":\"2026-09-01\",\"fechaFinEstimada\":\"2026-09-05\","
                + "\"detalles\":[{\"dosis\":10,\"unidadDosis\":\"ml\",\"productoTexto\":\"Oxitetraciclina LA\","
                + "\"frecuenciaHoras\":24,\"duracionDias\":5}]}";

        CrearTratamientoCommand command = json.readValue(body, CrearTratamientoRequest.class).command();

        assertThat(command.fechaInicio()).isEqualTo(LocalDate.of(2026, 9, 1).atStartOfDay(BOLIVIA).toInstant());
        assertThat(command.fechaFinEstimada()).isEqualTo(LocalDate.of(2026, 9, 5).atStartOfDay(BOLIVIA).toInstant());
        assertThat(command.detalles().get(0).productoTexto()).isEqualTo("Oxitetraciclina LA");
    }

    @Test
    void tratamientoCombinaFechaYHoraDeInicio() {
        String body = "{\"animalId\":\"70000000-0000-0000-0000-000000000001\","
                + "\"fechaInicio\":\"2026-09-01\",\"horaInicio\":\"08:30\",\"fechaFinEstimada\":\"2026-09-05\","
                + "\"detalles\":[{\"dosis\":10,\"unidadDosis\":\"ml\",\"frecuenciaHoras\":24,\"duracionDias\":5}]}";

        CrearTratamientoCommand command = json.readValue(body, CrearTratamientoRequest.class).command();

        assertThat(command.fechaInicio()).isEqualTo(LocalDate.of(2026, 9, 1).atTime(8, 30).atZone(BOLIVIA).toInstant());
    }
}
