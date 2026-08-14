package bo.com.ganadero.sanidad.domain; import java.util.*;
public interface SanidadRepository {
 List<Enfermedad> enfermedades(UUID empresa,boolean incluirInactivas); Optional<Enfermedad> enfermedad(UUID id,UUID empresa);
 Enfermedad crearEnfermedad(Enfermedad value); Enfermedad cambiarEstadoEnfermedad(UUID id,UUID empresa,boolean activo);
 List<PlanSanitario> planes(UUID empresa); Optional<PlanSanitario> plan(UUID id,UUID empresa); PlanSanitario crearPlan(PlanSanitario value,UUID actor);
 PlanSanitario cambiarEstadoPlan(UUID id,UUID empresa,EstadoPlanSanitario estado,long version,UUID actor);
 List<PlanSanitarioItem> items(UUID planId,UUID empresa,boolean incluirInactivos); PlanSanitarioItem crearItem(PlanSanitarioItem value,UUID actor);
 PlanSanitarioItem cambiarEstadoItem(UUID id,UUID planId,UUID empresa,boolean activo,long version,UUID actor);
}
