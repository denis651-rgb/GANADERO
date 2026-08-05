package bo.com.ganadero.animales.domain;

import java.time.Instant;
import java.util.UUID;

public record Parentesco(
        UUID id,
        UUID empresaId,
        UUID animalId,
        TipoParentesco tipo,
        UUID animalPadreId,
        String nombreExterno,
        UUID razaExternaId,
        String registroGenealogico,
        Instant fechaRegistro,
        UUID registradoPor) {
}
