package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.application.NuevoLoteCommand;
import jakarta.validation.constraints.Size;

public record NuevoLoteRequest(@Size(max = 160) String nombre, @Size(max = 60) String codigo,
                               @Size(max = 1000) String descripcion) {
    public NuevoLoteCommand command() {
        return new NuevoLoteCommand(nombre, codigo, descripcion);
    }
}
