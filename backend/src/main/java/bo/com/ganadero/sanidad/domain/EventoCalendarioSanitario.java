package bo.com.ganadero.sanidad.domain;

import java.time.Instant;
import java.util.UUID;

/** Un evento proyectado/pendiente/realizado del calendario sanitario genérico (secciones 19-20). */
public record EventoCalendarioSanitario(
        UUID id, UUID empresaId, UUID actividadId, UUID animalId, String cicloClave, Instant fechaPrevista,
        Instant ventanaDesde, Instant ventanaHasta, EstadoEventoCalendario estado, ModalidadActividad origenModalidad,
        String hallazgoOrigenTipo, UUID hallazgoOrigenId, UUID jornadaId, UUID ocurrenciaId, String prioridad,
        Instant createdAt, long version) {
}
