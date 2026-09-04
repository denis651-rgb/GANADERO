package bo.com.ganadero.lotes.api;

import bo.com.ganadero.lotes.domain.MembresiaLote;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class MembresiaResponseTest {
    @Test
    void incluyeNombreYCodigoEnLaRespuestaDeMembresias() {
        var membresia = new MembresiaLote(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                Instant.now(), null, null, null, null, "ATOMICO", null, null, 0, "ANI-000001", "LB-01");
        var response = MembresiaResponse.from(membresia);
        assertThat(response.animalNombre()).isEqualTo("LB-01");
        assertThat(response.animalCodigo()).isEqualTo("ANI-000001");
    }
}
