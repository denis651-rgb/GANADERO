package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.application.IdentificadorCommand;
import bo.com.ganadero.animales.domain.TipoIdentificador;
import jakarta.validation.constraints.Size;

public record ActualizarIdentificadorRequest(
        TipoIdentificador tipo,
        @Size(max = 120) String valor,
        Boolean principal,
        @Size(max = 1000) String observaciones) {

    public IdentificadorCommand command() {
        return new IdentificadorCommand(null, tipo, valor, principal, observaciones);
    }
}
