package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.application.ReproduccionService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Historial reproductivo completo de un animal. Vive en el módulo
 * reproduccion para mantener la dirección de dependencia animales → reproduccion
 * respetada por el límite Modulith.
 */
@RestController
@RequestMapping("/api/v1/animales/{animalId}/reproduccion")
public class ReproduccionAnimalController {
    private final ReproduccionService service;

    public ReproduccionAnimalController(ReproduccionService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<ReproduccionAnimalResponse> reproduccion(@PathVariable UUID animalId, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ApiResponse.success(ReproduccionAnimalResponse.from(service.reproduccionAnimal(animalId)),
                value == null ? "unknown" : value.toString());
    }
}
