package bo.com.ganadero.sanidad.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoCalendarioSanitarioRepository {
    /** Inserta el evento; si ya existe uno con la misma clave de ciclo (o el mismo hallazgo de origen), no hace nada. */
    void crearSiNoExiste(EventoCalendarioSanitario evento);

    List<EventoCalendarioSanitario> listar(UUID empresa, EstadoEventoCalendario estado, UUID animalId,
                                          Instant desde, Instant hasta, int limit);

    Optional<EventoCalendarioSanitario> findPendientePorAnimalYActividad(UUID actividadId, UUID animalId);

    /** true si a la ocurrencia todavía le quedan eventos pendientes (PROYECTADO/PROGRAMADO/EN_PREPARACION). */
    boolean tienePendientes(UUID ocurrenciaId);

    void marcarEstado(UUID id, EstadoEventoCalendario estado, UUID jornadaId, UUID actor);
}
