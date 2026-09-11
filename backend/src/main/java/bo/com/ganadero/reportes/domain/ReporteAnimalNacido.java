package bo.com.ganadero.reportes.domain;

import bo.com.ganadero.animales.domain.SexoAnimal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Fila aplanada (nombres ya resueltos, no IDs de catálogo) para el reporte de nacimientos de un período. */
public record ReporteAnimalNacido(UUID animalId, String codigo, String nombre, SexoAnimal sexo,
                                  LocalDate fechaNacimiento, boolean fechaNacimientoEstimada, String raza,
                                  String categoria, BigDecimal pesoNacimientoKg, String propiedad, String potrero,
                                  String madre, String padre) {
}
