package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.domain.ReproduccionAnimal;

import java.util.List;
import java.util.UUID;

public record ReproduccionAnimalResponse(
        UUID animalId,
        List<CeloResponse> celos,
        List<ServicioResponse> servicios,
        List<DiagnosticoGestacionResponse> diagnosticos) {

    public static ReproduccionAnimalResponse from(ReproduccionAnimal r) {
        return new ReproduccionAnimalResponse(r.animalId(),
                r.celos().stream().map(CeloResponse::from).toList(),
                r.servicios().stream().map(ServicioResponse::from).toList(),
                r.diagnosticos().stream().map(DiagnosticoGestacionResponse::from).toList());
    }
}
