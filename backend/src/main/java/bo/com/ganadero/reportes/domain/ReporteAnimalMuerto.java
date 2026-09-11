package bo.com.ganadero.reportes.domain;

import bo.com.ganadero.animales.domain.SexoAnimal;

import java.time.LocalDate;
import java.util.UUID;

/** Fila aplanada para el reporte de bajas por muerte de un período (fecha y motivo salen del timeline del animal). */
public record ReporteAnimalMuerto(UUID animalId, String codigo, String nombre, SexoAnimal sexo, String raza,
                                  String categoria, LocalDate fechaMuerte, String motivo, String propiedad,
                                  String potrero) {
}
