package bo.com.ganadero.sanidad.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventoCalendarioSanitarioRepository {
    /** Inserta el evento; si ya existe uno con la misma clave de ciclo (o el mismo hallazgo de origen), no hace nada. */
    void crearSiNoExiste(EventoCalendarioSanitario evento);

    List<EventoCalendarioSanitario> listar(UUID empresa, EstadoEventoCalendario estado, UUID animalId,
                                          Instant desde, Instant hasta, int limit);

    /**
     * Eventos de un animal y actividad que una aplicación puede cerrar: los pendientes y también los
     * vencidos, porque aplicar tarde sigue cumpliendo la actividad. Ordenados por fecha prevista.
     */
    List<EventoCalendarioSanitario> cerrablesPorAplicacion(UUID actividadId, UUID animalId);

    /** true si a la ocurrencia todavía le quedan eventos pendientes (PROYECTADO/PROGRAMADO/EN_PREPARACION). */
    boolean tienePendientes(UUID ocurrenciaId);

    /**
     * true si a la ocurrencia le queda algún evento sin cerrar: pendiente, en preparación o vencido.
     * Los vencidos cuentan porque son animales que siguen sin cumplir la actividad.
     */
    boolean tieneSinCerrar(UUID ocurrenciaId);

    void marcarEstado(UUID id, EstadoEventoCalendario estado, UUID jornadaId, UUID actor);

    /**
     * Cancela los eventos todavía no iniciados (PROYECTADO/PROGRAMADO) de una actividad y devuelve
     * las ocurrencias que tocó. No toca EN_PREPARACION (ya están en una jornada), ni los cerrados
     * (REALIZADO/OMITIDO) ni los VENCIDO, que son historial.
     */
    List<UUID> cancelarPendientesDeActividad(UUID actividadId);

    /** Igual que {@link #cancelarPendientesDeActividad} pero para todas las actividades de un plan (al finalizarlo o anularlo). */
    List<UUID> cancelarPendientesDePlan(UUID planId);

    /**
     * Devuelve a PROYECTADO los eventos CANCELADOS de una actividad cuya fecha todavía no llegó
     * (al reactivarla): sin esto la clave única del calendario impediría regenerarlos. Los que ya
     * pasaron se dejan cancelados para no resucitar vencidos.
     */
    void restaurarCanceladosFuturos(UUID actividadId);

    /**
     * Eventos de una actividad que pueden entrar en la conciliación de su serie periódica:
     * los pendientes (PROYECTADO/PROGRAMADO) y los cancelados que todavía no llegaron a su fecha.
     */
    List<EventoCalendarioSanitario> pendientesOCanceladosFuturosDeActividad(UUID actividadId);
}
