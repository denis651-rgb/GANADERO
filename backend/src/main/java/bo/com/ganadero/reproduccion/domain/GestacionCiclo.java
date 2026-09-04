package bo.com.ganadero.reproduccion.domain;
import java.time.LocalDate;
import java.util.UUID;
public record GestacionCiclo(UUID id, UUID animalId, UUID servicioId, UUID diagnosticoId,
 boolean antecedentesDesconocidos, LocalDate fechaConfirmacion, LocalDate fechaInicioEstimada,
 String observaciones, String estado, LocalDate fechaCierre, UUID eventoId) {}
