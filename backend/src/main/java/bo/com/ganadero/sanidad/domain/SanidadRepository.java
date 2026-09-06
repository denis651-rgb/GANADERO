package bo.com.ganadero.sanidad.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SanidadRepository {
    List<Enfermedad> enfermedades(UUID empresa, boolean incluirInactivas);

    Optional<Enfermedad> enfermedad(UUID id, UUID empresa);

    Enfermedad crearEnfermedad(Enfermedad value);

    Enfermedad cambiarEstadoEnfermedad(UUID id, UUID empresa, boolean activo);

    List<PlanSanitario> planes(UUID empresa);

    Optional<PlanSanitario> plan(UUID id, UUID empresa);

    PlanSanitario crearPlan(PlanSanitario value, UUID actor);

    PlanSanitario cambiarEstadoPlan(UUID id, UUID empresa, EstadoPlanSanitario estado, long version, UUID actor);

    /** Otros planes ACTIVO en el mismo alcance (misma propiedad, o global si propiedadId es null). */
    List<PlanSanitario> planesActivosEnAlcance(UUID propiedadId, UUID empresa, UUID excluirPlanId);

    List<PlanSanitarioItem> items(UUID planId, UUID empresa, boolean incluirInactivos);

    Optional<PlanSanitarioItem> item(UUID id, UUID empresa);

    /** Todas las versiones (vigente e históricas) de la misma identidad lógica, más reciente primero. */
    List<PlanSanitarioItem> versiones(UUID identidadLogicaId, UUID empresa);

    PlanSanitarioItem crearItem(PlanSanitarioItem value, UUID actor);

    /** Actualización directa in-place (sólo válida si el item nunca fue usado). */
    PlanSanitarioItem actualizarItem(PlanSanitarioItem value, UUID actor);

    /** Cierra la vigencia de una versión (al crear la siguiente). */
    void cerrarVigenciaItem(UUID id, Instant vigenteHasta, long version, UUID actor);

    PlanSanitarioItem cambiarEstadoItem(UUID id, UUID planId, UUID empresa, boolean activo, long version, UUID actor);

    /** true si el item está referenciado por algún evento de calendario o aplicación sanitaria (histórico). */
    boolean itemEnUso(UUID id);
}
