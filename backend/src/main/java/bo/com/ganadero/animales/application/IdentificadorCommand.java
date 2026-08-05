package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.TipoIdentificador;

import java.util.UUID;

public record IdentificadorCommand(
        UUID id,
        TipoIdentificador tipo,
        String valor,
        Boolean principal,
        String observaciones) {
}
