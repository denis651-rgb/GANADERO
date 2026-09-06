package bo.com.ganadero.sanidad.domain;

import java.util.UUID;

public interface OcurrenciaCalendarioSanitarioRepository {

    /** Idempotente por {@code ocurrenciaClave}: si ya existe, devuelve el id existente sin duplicar. */
    UUID crearOUsar(OcurrenciaCalendarioSanitario ocurrencia);
}
