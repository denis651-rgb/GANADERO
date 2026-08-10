package bo.com.ganadero.pesajes.domain;

import java.util.UUID;

public record PesajeMasivoResultado(
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        boolean ok,
        Pesaje pesaje,
        String errorCode,
        String errorMessage) {

    public static PesajeMasivoResultado exito(Pesaje pesaje) {
        return new PesajeMasivoResultado(pesaje.animalId(), pesaje.codigoAnimal(), pesaje.nombreAnimal(),
                true, pesaje, null, null);
    }

    public static PesajeMasivoResultado error(UUID animalId, String codigoAnimal, String nombreAnimal,
                                              String errorCode, String errorMessage) {
        return new PesajeMasivoResultado(animalId, codigoAnimal, nombreAnimal, false, null, errorCode, errorMessage);
    }
}
