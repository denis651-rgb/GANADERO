package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.application.PesajeService;
import bo.com.ganadero.shared.api.ApiResponse;
import bo.com.ganadero.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Ruta canónica de pesajes de un animal. Vive en el módulo pesajes para
 * mantener la dirección de dependencia animales → pesajes respetada por
 * el límite Modulith.
 */
@RestController
@RequestMapping("/api/v1/animales/{animalId}/pesajes")
public class PesajeAnimalController {
    private final PesajeService service;

    public PesajeAnimalController(PesajeService service) {
        this.service = service;
    }

    @GetMapping
    ApiResponse<List<PesajeResponse>> history(@PathVariable UUID animalId, HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        List<PesajeResponse> data = service.history(animalId).stream().map(PesajeResponse::from).toList();
        return ApiResponse.success(data, value == null ? "unknown" : value.toString());
    }
}
