package bo.com.ganadero.reproduccion.domain;

import java.util.List;
import java.util.UUID;

public record ReproduccionAnimal(UUID animalId, List<Celo> celos, List<Servicio> servicios,
                                 List<DiagnosticoGestacion> diagnosticos) {
}
