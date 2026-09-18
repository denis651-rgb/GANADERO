package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Reglas de registro: no sustituyen la evaluación ni la prescripción veterinaria. */
final class ReglasSanitarias {
    static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private ReglasSanitarias() {}

    /**
     * Motivos por los que la edad del animal en {@code fecha} no cumple el rango de la actividad
     * («Edad de los animales elegibles»); vacío si cumple. Es el único criterio de edad: lo usan
     * tanto la elegibilidad al preparar la jornada como la generación del calendario, para que
     * el calendario no programe lo que la jornada luego rechazaría.
     */
    static List<String> motivosEdad(LocalDate fechaNacimiento, LocalDate fecha, Integer edadMinDias,
                                    Integer edadMaxDias, boolean permiteEdadDesconocida) {
        List<String> motivos = new ArrayList<>();
        Long edad = fechaNacimiento == null ? null : ChronoUnit.DAYS.between(fechaNacimiento, fecha);
        if ((edadMinDias != null || edadMaxDias != null) && edad == null && !permiteEdadDesconocida) {
            motivos.add("El animal no tiene fecha de nacimiento para validar su edad.");
        }
        if (edad != null && edadMinDias != null && edad < edadMinDias) {
            motivos.add("Tiene " + edad + " días; la actividad requiere al menos " + edadMinDias + " días.");
        }
        if (edad != null && edadMaxDias != null && edad > edadMaxDias) {
            motivos.add("Tiene " + edad + " días; la actividad permite como máximo " + edadMaxDias + " días.");
        }
        return motivos;
    }

    static void exigir(boolean condicion, String mensaje) {
        if (!condicion) throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, mensaje);
    }

    static void fechaRealizada(LocalDate fecha) {
        exigir(fecha != null && !fecha.isAfter(LocalDate.now(ZONA)),
                "La fecha del evento sanitario es obligatoria y no puede estar en el futuro.");
    }

    static void fechaAnimal(Animal animal, LocalDate fecha, boolean antecedente) {
        fechaRealizada(fecha);
        exigir(animal.fechaNacimiento() == null || !fecha.isBefore(animal.fechaNacimiento()),
                "El evento sanitario no puede ser anterior al nacimiento del animal.");
        if (!antecedente) exigir(animal.fechaIngreso() == null || !fecha.isBefore(animal.fechaIngreso()),
                "El evento en la finca no puede ser anterior al ingreso. Use historial declarado para antecedentes del proveedor.");
    }

    static void fechaAnimal(Animal animal, Instant fecha) {
        exigir(fecha != null && !fecha.isAfter(Instant.now()),
                "La fecha y hora del evento no pueden estar en el futuro.");
        fechaAnimal(animal, fecha.atZone(ZONA).toLocalDate(), false);
    }

    static void neonatal(Animal animal, CrearControlNeonatalCommand c) {
        fechaAnimal(animal, c.fechaControl(), false);
        exigir(animal.fechaNacimiento() != null && !animal.fechaNacimientoEstimada(),
                "El control neonatal requiere una fecha de nacimiento confirmada; con edad desconocida o estimada registre un caso clínico.");
        long dias = ChronoUnit.DAYS.between(animal.fechaNacimiento(), c.fechaControl());
        exigir(c.momento() != null && (c.momento() == MomentoControlNeonatal.DIA_0 ? dias == 0 : dias >= 1 && dias <= 7),
                "Día 0 corresponde al nacimiento; primera semana corresponde a los días 1 a 7. Fuera de ese período registre un caso clínico.");
    }

    /**
     * Genérico para cualquier tipo de actividad (no sólo vacunación): si el ítem no tiene
     * frecuencia configurada, no hay intervalo mínimo que exigir.
     */
    static void intervaloMinimoEntreAplicaciones(JornadaSanitariaRepository repo, UUID empresa, UUID animal,
                                                 PlanSanitarioItem item, UUID producto, LocalDate fecha) {
        if (item.frecuenciaDias() == null || item.frecuenciaDias() <= 0) return;
        for (AplicacionSanitaria anterior : repo.vacunacionesRelacionadas(empresa, animal, item, producto)) {
            long distancia = Math.abs(ChronoUnit.DAYS.between(anterior.fechaAplicacion(), fecha));
            exigir(distancia != 0, "Ya existe una aplicación de esta actividad para el animal en esa fecha.");
            exigir(distancia >= item.frecuenciaDias(),
                    "Debe respetar el intervalo configurado de " + item.frecuenciaDias()
                            + " días respecto de la aplicación del " + anterior.fechaAplicacion() + ".");
        }
    }
}
