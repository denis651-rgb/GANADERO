package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.application.ValidacionAnimalResult;

public record ValidacionAnimalResponse(String animalId, String estado, String codigo, String mensaje) {
    public static ValidacionAnimalResponse from(ValidacionAnimalResult resultado) {
        return new ValidacionAnimalResponse(resultado.animalId().toString(),
                resultado.valido() ? "VALIDO" : "INVALIDO",
                resultado.valido() ? null : resultado.error().name(),
                resultado.mensaje());
    }
}
