package bo.com.ganadero.movimientolote.infrastructure;

import bo.com.ganadero.movimientolote.domain.*;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcPreparacionMovimientoLoteRepository implements PreparacionMovimientoLoteRepository {
    private final JdbcClient jdbc;
    private final ObjectMapper json;

    JdbcPreparacionMovimientoLoteRepository(JdbcClient jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Override
    public PreparacionMovimientoLote crear(PreparacionMovimientoLote p, List<PreparacionMovimientoLoteMiembro> miembros,
                                           UUID actor) {
        jdbc.sql("""
                insert into preparacion_movimiento_lote(id,lote_origen_id,propiedad_origen_id,potrero_origen_id,
                    modalidad,destino_propiedad_id,destino_potrero_id,accion_lote,lote_destino_id,
                    nuevo_lote_nombre,nuevo_lote_codigo,nuevo_lote_descripcion,fecha_efectiva,motivo,observaciones,
                    estado,fecha_expiracion,created_by,updated_by)
                values(:id,:loteOrigen,:propOrigen,:potOrigen,:modalidad,:destProp,:destPot,:accion,:loteDestino,
                    :nombre,:codigo,:descripcion,:fechaEfectiva,:motivo,:observaciones,:estado,:expiracion,:actor,:actor)
                """).params(paramsPreparacion(p)).param("actor", actor.toString()).update();
        for (PreparacionMovimientoLoteMiembro m : miembros) {
            jdbc.sql("""
                    insert into preparacion_movimiento_lote_miembro(id,preparacion_id,animal_id,animal_codigo,
                        animal_nombre,animal_estado,propiedad_origen_id,potrero_origen_id,lote_origen_id,
                        animal_version,elegible,motivo_exclusion,seleccionado,restricciones)
                    values(:id,:prep,:animal,:codigo,:nombre,:estado,:propiedad,:potrero,:lote,:version,
                        :elegible,:motivo,:seleccionado,:restricciones)
                    """)
                    .param("id", m.id().toString()).param("prep", p.id().toString())
                    .param("animal", m.animalId().toString()).param("codigo", m.animalCodigo())
                    .param("nombre", m.animalNombre()).param("estado", m.animalEstado())
                    .param("propiedad", m.propiedadOrigenId() == null ? null : m.propiedadOrigenId().toString())
                    .param("potrero", m.potreroOrigenId() == null ? null : m.potreroOrigenId().toString())
                    .param("lote", m.loteOrigenId() == null ? null : m.loteOrigenId().toString())
                    .param("version", m.animalVersion()).param("elegible", m.elegible() ? 1 : 0)
                    .param("motivo", m.motivoExclusion()).param("seleccionado", m.seleccionado() ? 1 : 0)
                    .param("restricciones", write(m.restricciones())).update();
        }
        return findById(p.id()).orElseThrow();
    }

    @Override
    public Optional<PreparacionMovimientoLote> findById(UUID id) {
        return jdbc.sql("select * from preparacion_movimiento_lote where id=:id")
                .param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public Optional<PreparacionMovimientoLote> findByIdForUpdate(UUID id) {
        return findById(id);
    }

    @Override
    public List<PreparacionMovimientoLoteMiembro> findMiembros(UUID preparacionId) {
        return jdbc.sql("select * from preparacion_movimiento_lote_miembro where preparacion_id=:p order by animal_codigo")
                .param("p", preparacionId.toString()).query(this::mapMiembro).list();
    }

    @Override
    public PreparacionMovimientoLote marcarEstado(UUID id, EstadoPreparacionLote estado, long version, UUID actor) {
        int changed = jdbc.sql("""
                update preparacion_movimiento_lote set estado=:estado,
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version""")
                .param("estado", estado.name()).param("actor", actor.toString())
                .param("id", id.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(id);
        return findById(id).orElseThrow();
    }

    @Override
    public PreparacionMovimientoLote confirmar(UUID id, UUID movimientoResultanteId, UUID loteResultanteId,
                                               long version, UUID actor) {
        int changed = jdbc.sql("""
                update preparacion_movimiento_lote set estado='CONFIRMADA',
                    movimiento_resultante_id=:movimiento, lote_resultante_id=:lote,
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='VIGENTE'""")
                .param("movimiento", movimientoResultanteId == null ? null : movimientoResultanteId.toString())
                .param("lote", loteResultanteId == null ? null : loteResultanteId.toString())
                .param("actor", actor.toString()).param("id", id.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(id);
        return findById(id).orElseThrow();
    }

    @Override
    public void registrarAutorizacion(UUID preparacionId, UUID animalId, String tipoRestriccion, String motivo,
                                      UUID usuarioId) {
        jdbc.sql("""
                insert into preparacion_movimiento_lote_autorizacion(id,preparacion_id,animal_id,tipo_restriccion,
                    motivo,usuario_id)
                values(:id,:prep,:animal,:tipo,:motivo,:usuario)""")
                .param("id", UUID.randomUUID().toString()).param("prep", preparacionId.toString())
                .param("animal", animalId.toString()).param("tipo", tipoRestriccion).param("motivo", motivo)
                .param("usuario", usuarioId == null ? null : usuarioId.toString()).update();
    }

    @Override
    public List<AutorizacionRestriccion> findAutorizaciones(UUID preparacionId) {
        return jdbc.sql("select * from preparacion_movimiento_lote_autorizacion where preparacion_id=:p")
                .param("p", preparacionId.toString()).query(this::mapAutorizacion).list();
    }

    private Map<String, Object> paramsPreparacion(PreparacionMovimientoLote p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.id().toString());
        m.put("loteOrigen", p.loteOrigenId().toString());
        m.put("propOrigen", p.propiedadOrigenId() == null ? null : p.propiedadOrigenId().toString());
        m.put("potOrigen", p.potreroOrigenId() == null ? null : p.potreroOrigenId().toString());
        m.put("modalidad", p.modalidad().name());
        m.put("destProp", p.destinoPropiedadId().toString());
        m.put("destPot", p.destinoPotreroId().toString());
        m.put("accion", p.accionLote().name());
        m.put("loteDestino", p.loteDestinoId() == null ? null : p.loteDestinoId().toString());
        m.put("nombre", p.nuevoLoteNombre());
        m.put("codigo", p.nuevoLoteCodigo());
        m.put("descripcion", p.nuevoLoteDescripcion());
        m.put("fechaEfectiva", p.fechaEfectiva().toString());
        m.put("motivo", p.motivo());
        m.put("observaciones", p.observaciones());
        m.put("estado", p.estado().name());
        m.put("expiracion", p.fechaExpiracion().toString());
        return m;
    }

    private PreparacionMovimientoLote map(ResultSet r, int n) throws SQLException {
        String modalidad = r.getString("modalidad");
        String accion = r.getString("accion_lote");
        String estado = r.getString("estado");
        return new PreparacionMovimientoLote(Rows.uuid(r, "id"), Rows.uuid(r, "lote_origen_id"),
                Rows.uuid(r, "propiedad_origen_id"), Rows.uuid(r, "potrero_origen_id"),
                modalidad == null ? null : bo.com.ganadero.movimientolote.domain.ModalidadMovimientoLote.valueOf(modalidad),
                Rows.uuid(r, "destino_propiedad_id"), Rows.uuid(r, "destino_potrero_id"),
                accion == null ? null : AccionLote.valueOf(accion), Rows.uuid(r, "lote_destino_id"),
                r.getString("nuevo_lote_nombre"), r.getString("nuevo_lote_codigo"), r.getString("nuevo_lote_descripcion"),
                Rows.instant(r, "fecha_efectiva"), r.getString("motivo"), r.getString("observaciones"),
                estado == null ? null : EstadoPreparacionLote.valueOf(estado),
                Rows.instant(r, "fecha_captura"), Rows.instant(r, "fecha_expiracion"),
                Rows.uuid(r, "movimiento_resultante_id"), Rows.uuid(r, "lote_resultante_id"),
                Rows.uuid(r, "created_by"), r.getLong("version"));
    }

    private PreparacionMovimientoLoteMiembro mapMiembro(ResultSet r, int n) throws SQLException {
        return new PreparacionMovimientoLoteMiembro(Rows.uuid(r, "id"), Rows.uuid(r, "preparacion_id"),
                Rows.uuid(r, "animal_id"), r.getString("animal_codigo"), r.getString("animal_nombre"),
                r.getString("animal_estado"), Rows.uuid(r, "propiedad_origen_id"), Rows.uuid(r, "potrero_origen_id"),
                Rows.uuid(r, "lote_origen_id"), r.getLong("animal_version"), r.getBoolean("elegible"),
                r.getString("motivo_exclusion"), r.getBoolean("seleccionado"), read(r.getString("restricciones")),
                Rows.instant(r, "fecha_captura"));
    }

    private AutorizacionRestriccion mapAutorizacion(ResultSet r, int n) throws SQLException {
        return new AutorizacionRestriccion(Rows.uuid(r, "id"), Rows.uuid(r, "preparacion_id"),
                Rows.uuid(r, "animal_id"), r.getString("tipo_restriccion"), r.getString("motivo"),
                Rows.uuid(r, "usuario_id"), Rows.instant(r, "fecha"));
    }

    private String write(List<RestriccionSanitaria> restricciones) {
        try {
            return json.writeValueAsString(restricciones == null ? List.of() : restricciones);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    private List<RestriccionSanitaria> read(String s) {
        try {
            return s == null ? List.of() : json.readValue(s, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private BusinessException missingOrConflict(UUID id) {
        boolean exists = findById(id).isPresent();
        return new BusinessException(exists ? ErrorCode.VERSION_CONFLICT : ErrorCode.PREPARACION_LOTE_NOT_FOUND);
    }
}
