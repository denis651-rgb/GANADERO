package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.Parentesco;
import bo.com.ganadero.animales.domain.TipoParentesco;

import java.time.Instant;
import java.util.UUID;

public record ParentescoResponse(
        UUID id,
        UUID animalId,
        TipoParentesco tipo,
        UUID animalPadreId,
        String nombreExterno,
        UUID razaExternaId,
        String registroGenealogico,
        Instant fechaRegistro) {

    public static ParentescoResponse from(Parentesco p) {
        return new ParentescoResponse(p.id(), p.animalId(), p.tipo(), p.animalPadreId(), p.nombreExterno(),
                p.razaExternaId(), p.registroGenealogico(), p.fechaRegistro());
    }
}
