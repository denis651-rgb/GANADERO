package bo.com.ganadero.sanidad.infrastructure;

import bo.com.ganadero.sanidad.domain.EstadoEventoCalendario;
import bo.com.ganadero.sanidad.domain.EventoCalendarioSanitario;
import bo.com.ganadero.sanidad.domain.EventoCalendarioSanitarioRepository;
import bo.com.ganadero.sanidad.domain.ModalidadActividad;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A diferencia de las tablas viejas de sanidad (sin empresa_id real, aislamiento delegado a la
 * capa de aplicación), esta tabla es nueva: filtra empresa_id de verdad en el propio SQL.
 */
@Repository
public class JdbcEventoCalendarioSanitarioRepository implements EventoCalendarioSanitarioRepository {
    private final JdbcClient jdbc;

    public JdbcEventoCalendarioSanitarioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void crearSiNoExiste(EventoCalendarioSanitario evento) {
        jdbc.sql("""
                insert into evento_calendario_sanitario
                    (id, empresa_id, actividad_id, animal_id, ciclo_clave, fecha_prevista, ventana_desde,
                     ventana_hasta, estado, origen_modalidad, hallazgo_origen_tipo, hallazgo_origen_id,
                     ocurrencia_id, prioridad)
                values (:id, :empresa, :act, :animal, :ciclo, :fecha, :vd, :vh, :estado, :modalidad, :hOrigen, :hId,
                     :ocurrencia, :prioridad)
                on conflict do nothing
                """)
                .param("id", evento.id().toString())
                .param("empresa", evento.empresaId() == null ? null : evento.empresaId().toString())
                .param("act", evento.actividadId().toString())
                .param("animal", evento.animalId().toString())
                .param("ciclo", evento.cicloClave())
                .param("fecha", evento.fechaPrevista().toString())
                .param("vd", evento.ventanaDesde() == null ? null : evento.ventanaDesde().toString())
                .param("vh", evento.ventanaHasta() == null ? null : evento.ventanaHasta().toString())
                .param("estado", evento.estado().name())
                .param("modalidad", evento.origenModalidad().name())
                .param("hOrigen", evento.hallazgoOrigenTipo())
                .param("hId", evento.hallazgoOrigenId() == null ? null : evento.hallazgoOrigenId().toString())
                .param("ocurrencia", evento.ocurrenciaId() == null ? null : evento.ocurrenciaId().toString())
                .param("prioridad", evento.prioridad())
                .update();
    }

    @Override
    public List<EventoCalendarioSanitario> listar(UUID empresa, EstadoEventoCalendario estado, UUID animalId,
                                                  Instant desde, Instant hasta, int limit) {
        StringBuilder sql = new StringBuilder("select * from evento_calendario_sanitario where empresa_id=:empresa");
        if (estado != null) sql.append(" and estado=:estado");
        if (animalId != null) sql.append(" and animal_id=:animal");
        if (desde != null) sql.append(" and fecha_prevista>=:desde");
        if (hasta != null) sql.append(" and fecha_prevista<=:hasta");
        sql.append(" order by fecha_prevista asc limit :limit");
        var q = jdbc.sql(sql.toString()).param("empresa", empresa.toString());
        if (estado != null) q = q.param("estado", estado.name());
        if (animalId != null) q = q.param("animal", animalId.toString());
        if (desde != null) q = q.param("desde", desde.toString());
        if (hasta != null) q = q.param("hasta", hasta.toString());
        q = q.param("limit", limit);
        return q.query(this::map).list();
    }

    @Override
    public List<EventoCalendarioSanitario> cerrablesPorAplicacion(UUID actividadId, UUID animalId) {
        return jdbc.sql("""
                select * from evento_calendario_sanitario
                where actividad_id=:act and animal_id=:animal
                    and estado in ('PROYECTADO','PROGRAMADO','EN_PREPARACION','VENCIDO')
                order by fecha_prevista asc
                """)
                .param("act", actividadId.toString()).param("animal", animalId.toString())
                .query(this::map).list();
    }

    @Override
    public boolean tienePendientes(UUID ocurrenciaId) {
        if (ocurrenciaId == null) return false;
        return jdbc.sql("""
                select count(*) from evento_calendario_sanitario
                where ocurrencia_id=:ocurrencia and estado in ('PROYECTADO','PROGRAMADO','EN_PREPARACION')
                """)
                .param("ocurrencia", ocurrenciaId.toString())
                .query(Integer.class).single() > 0;
    }

    @Override
    public boolean tieneSinCerrar(UUID ocurrenciaId) {
        if (ocurrenciaId == null) return false;
        return jdbc.sql("""
                select count(*) from evento_calendario_sanitario
                where ocurrencia_id=:ocurrencia and estado in ('PROYECTADO','PROGRAMADO','EN_PREPARACION','VENCIDO')
                """)
                .param("ocurrencia", ocurrenciaId.toString())
                .query(Integer.class).single() > 0;
    }

    @Override
    public void marcarEstado(UUID id, EstadoEventoCalendario estado, UUID jornadaId, UUID actor) {
        jdbc.sql("""
                update evento_calendario_sanitario
                set estado=:estado, jornada_id=coalesce(:jornada, jornada_id),
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), version=version+1
                where id=:id
                """)
                .param("estado", estado.name())
                .param("jornada", jornadaId == null ? null : jornadaId.toString())
                .param("id", id.toString())
                .update();
    }

    @Override
    public List<UUID> cancelarPendientesDeActividad(UUID actividadId) {
        List<UUID> ocurrencias = jdbc.sql("""
                select distinct ocurrencia_id from evento_calendario_sanitario
                where actividad_id=:act and estado in ('PROYECTADO','PROGRAMADO') and ocurrencia_id is not null
                """)
                .param("act", actividadId.toString())
                .query((r, n) -> UUID.fromString(r.getString("ocurrencia_id"))).list();
        jdbc.sql("""
                update evento_calendario_sanitario
                set estado='CANCELADO', updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), version=version+1
                where actividad_id=:act and estado in ('PROYECTADO','PROGRAMADO')
                """)
                .param("act", actividadId.toString())
                .update();
        return ocurrencias;
    }

    @Override
    public List<UUID> cancelarPendientesDePlan(UUID planId) {
        String deLaPlan = "actividad_id in (select id from plan_sanitario_item where plan_id=:plan)";
        List<UUID> ocurrencias = jdbc.sql("select distinct ocurrencia_id from evento_calendario_sanitario where "
                        + deLaPlan + " and estado in ('PROYECTADO','PROGRAMADO') and ocurrencia_id is not null")
                .param("plan", planId.toString())
                .query((r, n) -> UUID.fromString(r.getString("ocurrencia_id"))).list();
        jdbc.sql("update evento_calendario_sanitario set estado='CANCELADO',"
                        + " updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), version=version+1"
                        + " where " + deLaPlan + " and estado in ('PROYECTADO','PROGRAMADO')")
                .param("plan", planId.toString()).update();
        return ocurrencias;
    }

    @Override
    public List<EventoCalendarioSanitario> pendientesOCanceladosFuturosDeActividad(UUID actividadId) {
        return jdbc.sql("""
                select * from evento_calendario_sanitario
                where actividad_id=:act
                    and (estado in ('PROYECTADO','PROGRAMADO') or (estado='CANCELADO' and fecha_prevista >= :ahora))
                """)
                .param("act", actividadId.toString()).param("ahora", Instant.now().toString())
                .query(this::map).list();
    }

    @Override
    public void restaurarCanceladosFuturos(UUID actividadId) {
        jdbc.sql("""
                update evento_calendario_sanitario
                set estado='PROYECTADO', updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), version=version+1
                where actividad_id=:act and estado='CANCELADO' and fecha_prevista >= :ahora
                """)
                .param("act", actividadId.toString()).param("ahora", Instant.now().toString()).update();
    }

    private EventoCalendarioSanitario map(ResultSet r, int n) throws SQLException {
        return new EventoCalendarioSanitario(Rows.uuid(r, "id"), Rows.uuid(r, "empresa_id"), Rows.uuid(r, "actividad_id"),
                Rows.uuid(r, "animal_id"), r.getString("ciclo_clave"), Rows.instant(r, "fecha_prevista"),
                Rows.instant(r, "ventana_desde"), Rows.instant(r, "ventana_hasta"),
                EstadoEventoCalendario.valueOf(r.getString("estado")),
                ModalidadActividad.valueOf(r.getString("origen_modalidad")),
                r.getString("hallazgo_origen_tipo"), Rows.uuid(r, "hallazgo_origen_id"),
                Rows.uuid(r, "jornada_id"), Rows.uuid(r, "ocurrencia_id"), r.getString("prioridad"),
                Rows.instant(r, "created_at"), r.getLong("version"));
    }
}
