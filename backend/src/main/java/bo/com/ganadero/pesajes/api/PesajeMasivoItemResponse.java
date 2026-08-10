package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.pesajes.domain.PesajeMasivoResultado;

import java.util.List;
import java.util.UUID;

public record PesajeMasivoItemResponse(
        UUID animalId,
        String codigoAnimal,
        String nombreAnimal,
        boolean ok,
        PesajeResponse pesaje,
        String errorCode,
        String errorMessage) {

    public static PesajeMasivoItemResponse from(PesajeMasivoResultado resultado) {
        Pesaje pesaje = resultado.pesaje();
        return new PesajeMasivoItemResponse(resultado.animalId(), resultado.codigoAnimal(), resultado.nombreAnimal(),
                resultado.ok(), pesaje == null ? null : PesajeResponse.from(pesaje),
                resultado.errorCode(), resultado.errorMessage());
    }
}
