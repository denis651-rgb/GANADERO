package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.FuenteEdadDeclarada;
import bo.com.ganadero.animales.domain.UnidadEdadDeclarada;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;

import java.time.LocalDate;

/**
 * Resuelve fecha de nacimiento conocida, edad aproximada declarada (compra/campo) o edad
 * desconocida. El dato original declarado se conserva estructurado — nunca se mezcla con las
 * observaciones libres del usuario, para poder mostrarlo y corregirlo después.
 */
final class EstimacionEdadAnimal {
    private EstimacionEdadAnimal() {}

    static Resultado resolver(LocalDate fechaNacimiento, Boolean estimada, Integer valor,
                              UnidadEdadDeclarada unidad, LocalDate referencia,
                              FuenteEdadDeclarada fuente, String detalle) {
        if (valor == null) {
            return new Resultado(fechaNacimiento, Boolean.TRUE.equals(estimada), null, null, null, null, null);
        }
        if (fechaNacimiento != null) throw error("Indica una fecha de nacimiento o una edad aproximada, no ambas.");
        if (valor <= 0 || unidad == null || referencia == null || fuente == null) {
            throw error("La edad aproximada requiere valor, unidad, fecha de referencia y fuente.");
        }
        if (referencia.isAfter(LocalDate.now(java.time.ZoneId.of("America/La_Paz")))) {
            throw error("La fecha de referencia de la edad no puede estar en el futuro.");
        }
        LocalDate nacimiento = switch (unidad) {
            case DIAS -> referencia.minusDays(valor);
            case MESES -> referencia.minusMonths(valor);
            case ANIOS -> referencia.minusYears(valor);
        };
        return new Resultado(nacimiento, true, valor, unidad, referencia, fuente, detalle);
    }

    private static BusinessException error(String message) {
        return new BusinessException(ErrorCode.VALIDATION_ERROR, message);
    }

    record Resultado(LocalDate fechaNacimiento, boolean estimada, Integer valorDeclarado,
                     UnidadEdadDeclarada unidad, LocalDate fechaReferencia, FuenteEdadDeclarada fuente,
                     String detalle) {}
}
