package bo.com.ganadero.movimientos.application;
import java.time.LocalDate;
import java.util.UUID;
/** Puerto hacia Sanidad. Movimientos nunca consulta directamente sus tablas internas. */
public interface EstadoSanitarioIngresoPort {
 boolean tienePruebaDiagnosticaDesde(UUID empresaId, UUID animalId, LocalDate desde);
}
