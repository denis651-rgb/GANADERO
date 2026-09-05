package bo.com.ganadero.movimientolote.api;

import bo.com.ganadero.movimientolote.domain.RestriccionSanitaria;

public record RestriccionSanitariaResponse(String tipo, String severidad, String mensaje) {
    public static RestriccionSanitariaResponse from(RestriccionSanitaria r) {
        return new RestriccionSanitariaResponse(r.tipo(), r.severidad().name(), r.mensaje());
    }
}
