package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.TipoParentesco;

import java.util.UUID;

public record ParentescoCommand(
        TipoParentesco tipo,
        UUID animalPadreId,
        String nombreExterno,
        UUID razaExternaId,
        String registroGenealogico) {
}
