package bo.com.ganadero.animales.domain;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

public record CategoriaAnimal(UUID id, UUID empresaId, String codigo, String nombre, String sexoAplicable,
                              Integer edadMinMeses, Integer edadMaxMeses, String descripcion, boolean activo,
                              /** Categorías excepcionales (p. ej. Buey: depende de castración) llevan false aquí y nunca se deducen solo por edad. */
                              boolean clasificacionAutomatica, int ordenEvaluacion) {
    public CategoriaAnimal(UUID id, UUID empresaId, String codigo, String nombre, String sexoAplicable,
                           Integer edadMinMeses, Integer edadMaxMeses, String descripcion, boolean activo) {
        this(id, empresaId, codigo, nombre, sexoAplicable, edadMinMeses, edadMaxMeses, descripcion, activo,
                !"BUEY".equals(codigo), 0);
    }

    public boolean appliesTo(SexoAnimal sexo) {
        return "AMBOS".equals(sexoAplicable) || sexo.name().equals(sexoAplicable);
    }

    public boolean appliesTo(SexoAnimal sexo, LocalDate nacimiento, LocalDate referencia) {
        if (!appliesTo(sexo) || nacimiento == null || referencia == null || nacimiento.isAfter(referencia)) return false;
        Period edad = Period.between(nacimiento, referencia);
        int meses = edad.getYears() * 12 + edad.getMonths();
        return meses >= (edadMinMeses == null ? 0 : edadMinMeses)
                && (edadMaxMeses == null || meses <= edadMaxMeses);
    }
}
