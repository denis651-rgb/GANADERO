package bo.com.ganadero.movimientolote.application;

import bo.com.ganadero.movimientolote.domain.RestriccionSanitaria;

import java.util.List;
import java.util.UUID;

/**
 * Puerto hacia sanidad (docs: nunca acceder a la infraestructura interna de otro módulo).
 * Implementado en sanidad.infrastructure con SQL directo, mismo estilo que
 * {@code EstadoSanitarioIngresoPort}/{@code EstadoSanitarioIngresoAdapter} de movimientos.
 */
public interface RestriccionSanitariaPort {
    List<RestriccionSanitaria> evaluar(UUID empresaId, UUID animalId);
}
