package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.ParentescoCommand;
import bo.com.ganadero.animales.domain.TipoParentesco;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CrearParentescoRequest(
        @NotNull TipoParentesco tipo,
        UUID animalPadreId,
        @Size(max = 160) String nombreExterno,
        UUID razaExternaId,
        @Size(max = 160) String registroGenealogico) {

    public ParentescoCommand command() {
        return new ParentescoCommand(tipo, animalPadreId, nombreExterno, razaExternaId, registroGenealogico);
    }
}
