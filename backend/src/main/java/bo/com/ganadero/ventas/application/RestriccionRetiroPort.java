package bo.com.ganadero.ventas.application;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto hacia sanidad (sección 24): permite a {@link VentaService} comprobar restricciones de
 * retiro sin que este módulo dependa de sanidad — lo implementa un adaptador en
 * {@code sanidad.infrastructure}, mismo patrón que {@code RestriccionSanitariaPort}
 * (movimientolote) y {@code EstadoSanitarioIngresoPort} (movimientos).
 */
public interface RestriccionRetiroPort {
    Optional<RestriccionRetiroVigente> vigente(UUID empresaId, UUID animalId, LocalDate fechaVenta);

    record RestriccionRetiroVigente(String tipo, LocalDate hasta) {
    }
}
