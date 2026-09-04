package bo.com.ganadero.movimientos.infrastructure;

import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.movimientos.domain.*;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcMovimientoRepository implements MovimientoRepository {
    private final JdbcClient jdbc;

    JdbcMovimientoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public MovimientoPage findAll(UUID empresa, EstadoMovimiento estado, TipoMovimiento tipo, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (estado != null) { where.append(" and m.estado=:estado"); params.put("estado", estado.name()); }
        if (tipo != null) { where.append(" and m.tipo=:tipo"); params.put("tipo", tipo.name()); }
        long total = jdbc.sql("select count(*) from movimiento m" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<Movimiento> values = jdbc.sql("select m.* from movimiento m" + where
                        + " order by m.created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return MovimientoPage.of(values, page, size, total);
    }

    @Override
    public Optional<Movimiento> findById(UUID id, UUID empresa) {
        return jdbc.sql("select * from movimiento where id=:id")
                .param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public Optional<Movimiento> findByIdForUpdate(UUID id, UUID empresa) {
        return jdbc.sql("select * from movimiento where id=:id")
                .param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public Optional<Movimiento> findByOriginal(UUID id, UUID empresa) {
        return jdbc.sql("select * from movimiento where movimiento_revertido_id=:id")
                .param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public List<MovimientoDetalle> findDetalles(UUID movimientoId) {
        return jdbc.sql("select * from movimiento_detalle where movimiento_id=:m")
                .param("m", movimientoId.toString()).query(this::mapDetalle).list();
    }

    @Override
    public Movimiento create(Movimiento movimiento, List<MovimientoAnimal> animales, UUID actor) {
        jdbc.sql("""
                insert into movimiento(id,tipo,estado,fecha_movimiento,motivo,observacion,
                    origen_propiedad_id,origen_potrero_id,origen_lote_id,destino_propiedad_id,destino_potrero_id,
                    destino_lote_id,usuario_crea,created_by,updated_by)
                values(:id,:tipo,:estado,:fecha,:motivo,:obs,:opp,:opr,:ol,:dpp,:dpotrero,:dl,:actor,:actor,:actor)""")
                .param("id", movimiento.id().toString())
                .param("tipo", movimiento.tipo().name()).param("estado", movimiento.estado().name())
                .param("fecha", movimiento.fechaMovimiento() == null ? null : movimiento.fechaMovimiento().toString()).param("motivo", movimiento.motivo())
                .param("obs", movimiento.observacion())
                .param("opp", movimiento.origenPropiedadId() == null ? null : movimiento.origenPropiedadId().toString())
                .param("opr", movimiento.origenPotreroId() == null ? null : movimiento.origenPotreroId().toString())
                .param("ol", movimiento.origenLoteId() == null ? null : movimiento.origenLoteId().toString())
                .param("dpp", movimiento.destinoPropiedadId() == null ? null : movimiento.destinoPropiedadId().toString())
                .param("dpotrero", movimiento.destinoPotreroId() == null ? null : movimiento.destinoPotreroId().toString())
                .param("dl", movimiento.destinoLoteId() == null ? null : movimiento.destinoLoteId().toString())
                .param("actor", actor.toString()).update();
        insertDetalles(movimiento, animales);
        return findById(movimiento.id(), movimiento.empresaId()).orElseThrow();
    }

    @Override
    public Movimiento saveConfirmed(Movimiento movimiento, List<MovimientoAnimal> animales, UUID actor) {
        jdbc.sql("""
                insert into movimiento(id,tipo,estado,fecha_movimiento,motivo,observacion,
                    origen_propiedad_id,origen_potrero_id,origen_lote_id,destino_propiedad_id,destino_potrero_id,
                    destino_lote_id,usuario_crea,usuario_confirma,fecha_confirmacion,movimiento_revertido_id,
                    created_by,updated_by)
                values(:id,:tipo,'CONFIRMADO',:fecha,:motivo,:obs,:opp,:opr,:ol,:dpp,:dpotrero,:dl,:actor,:actor,
                    strftime('%Y-%m-%dT%H:%M:%fZ','now'),:rev,:actor,:actor)""")
                .param("id", movimiento.id().toString())
                .param("tipo", movimiento.tipo().name())
                .param("fecha", movimiento.fechaMovimiento() == null ? null : movimiento.fechaMovimiento().toString()).param("motivo", movimiento.motivo())
                .param("obs", movimiento.observacion())
                .param("opp", movimiento.origenPropiedadId() == null ? null : movimiento.origenPropiedadId().toString())
                .param("opr", movimiento.origenPotreroId() == null ? null : movimiento.origenPotreroId().toString())
                .param("ol", movimiento.origenLoteId() == null ? null : movimiento.origenLoteId().toString())
                .param("dpp", movimiento.destinoPropiedadId() == null ? null : movimiento.destinoPropiedadId().toString())
                .param("dpotrero", movimiento.destinoPotreroId() == null ? null : movimiento.destinoPotreroId().toString())
                .param("dl", movimiento.destinoLoteId() == null ? null : movimiento.destinoLoteId().toString())
                .param("rev", movimiento.movimientoRevertidoId() == null ? null : movimiento.movimientoRevertidoId().toString())
                .param("actor", actor.toString()).update();
        insertDetalles(movimiento, animales);
        return findById(movimiento.id(), movimiento.empresaId()).orElseThrow();
    }

    private void insertDetalles(Movimiento movimiento, List<MovimientoAnimal> animales) {
        for (MovimientoAnimal animal : animales) {
            jdbc.sql("insert into movimiento_detalle(id,movimiento_id,animal_id,animal_version_esperada,estado_antes,estado_despues) values(:id,:m,:animal,:ave,:antes,:despues)")
                    .param("id", UUID.randomUUID().toString()).param("m", movimiento.id().toString()).param("animal", animal.animalId().toString())
                    .param("ave", animal.version())
                    .param("antes", "ACTIVO").param("despues", "ACTIVO").update();
        }
    }

    @Override
    public Movimiento confirm(UUID id, UUID empresa, long version, UUID actor) {
        int changed = jdbc.sql("""
                update movimiento set estado='CONFIRMADO',usuario_confirma=:actor,fecha_confirmacion=strftime('%Y-%m-%dT%H:%M:%fZ','now'),
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='PENDIENTE'""")
                .param("actor", actor.toString()).param("id", id.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(id, empresa);
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public Movimiento annul(UUID id, UUID empresa, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update movimiento set estado='ANULADO',usuario_anula=:actor,fecha_anulacion=strftime('%Y-%m-%dT%H:%M:%fZ','now'),
                    motivo_anulacion=:motivo,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='PENDIENTE'""")
                .param("actor", actor.toString()).param("id", id.toString()).param("version", version)
                .param("motivo", motivo).update();
        if (changed == 0) throw missingOrConflict(id, empresa);
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public Movimiento markReverted(UUID id, UUID empresa, UUID reversionId, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update movimiento set estado='REVERTIDO',usuario_revierte=:actor,fecha_reversion=strftime('%Y-%m-%dT%H:%M:%fZ','now'),
                    motivo_reversion=:motivo,movimiento_reversion_id=:rev,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,
                    version=version+1
                where id=:id and version=:version and estado='CONFIRMADO'""")
                .param("actor", actor.toString()).param("id", id.toString()).param("version", version)
                .param("motivo", motivo).param("rev", reversionId == null ? null : reversionId.toString()).update();
        if (changed == 0) throw missingOrConflict(id, empresa);
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public void saveDetalleUbicaciones(UUID movimientoId, List<MovimientoDetalle> detalle) {
        for (MovimientoDetalle d : detalle) {
            jdbc.sql("""
                    update movimiento_detalle set animal_version_esperada=:ave,
                        propiedad_antes=:pa,potrero_antes=:pr,lote_antes=:la,
                        propiedad_despues=:pd,potrero_despues=:prd,lote_despues=:ld,
                        estado_resultado=:er,mensaje_resultado=:mr
                    where id=:id""")
                    .param("ave", d.animalVersionEsperada())
                    .param("pa", d.propiedadAntes() == null ? null : d.propiedadAntes().toString())
                    .param("pr", d.potreroAntes() == null ? null : d.potreroAntes().toString())
                    .param("la", d.loteAntes() == null ? null : d.loteAntes().toString())
                    .param("pd", d.propiedadDespues() == null ? null : d.propiedadDespues().toString())
                    .param("prd", d.potreroDespues() == null ? null : d.potreroDespues().toString())
                    .param("ld", d.loteDespues() == null ? null : d.loteDespues().toString())
                    .param("er", d.estadoResultado()).param("mr", d.mensajeResultado())
                    .param("id", d.id().toString()).update();
        }
    }

    private Movimiento map(ResultSet rs, int rowNum) throws SQLException {
        Instant confirmacion = Rows.instant(rs, "fecha_confirmacion");
        Instant anulacion = Rows.instant(rs, "fecha_anulacion");
        Instant reversion = Rows.instant(rs, "fecha_reversion");
        return new Movimiento(
                Rows.uuid(rs, "id"), null,
                TipoMovimiento.valueOf(rs.getString("tipo")), EstadoMovimiento.valueOf(rs.getString("estado")),
                rs.getString("fecha_movimiento") == null ? null : LocalDate.parse(rs.getString("fecha_movimiento")), rs.getString("motivo"),
                rs.getString("observacion"),
                Rows.uuid(rs, "origen_propiedad_id"), Rows.uuid(rs, "origen_potrero_id"),
                Rows.uuid(rs, "origen_lote_id"), Rows.uuid(rs, "destino_propiedad_id"),
                Rows.uuid(rs, "destino_potrero_id"), Rows.uuid(rs, "destino_lote_id"),
                Rows.uuid(rs, "usuario_crea"), Rows.uuid(rs, "usuario_confirma"),
                Rows.uuid(rs, "usuario_anula"), confirmacion, anulacion,
                rs.getString("motivo_anulacion"), Rows.uuid(rs, "usuario_revierte"),
                reversion, rs.getString("motivo_reversion"),
                Rows.uuid(rs, "movimiento_revertido_id"),
                Rows.uuid(rs, "movimiento_reversion_id"),
                rs.getLong("version"));
    }

    private MovimientoDetalle mapDetalle(ResultSet rs, int rowNum) throws SQLException {
        String antes = rs.getString("estado_antes");
        String despues = rs.getString("estado_despues");
        return new MovimientoDetalle(
                Rows.uuid(rs, "id"), Rows.uuid(rs, "movimiento_id"),
                Rows.uuid(rs, "animal_id"), rs.getLong("animal_version_esperada"),
                antes == null ? null : EstadoAnimal.valueOf(antes),
                despues == null ? null : EstadoAnimal.valueOf(despues),
                Rows.uuid(rs, "propiedad_antes"), Rows.uuid(rs, "potrero_antes"),
                Rows.uuid(rs, "lote_antes"), Rows.uuid(rs, "propiedad_despues"),
                Rows.uuid(rs, "potrero_despues"), Rows.uuid(rs, "lote_despues"),
                rs.getString("estado_resultado"), rs.getString("mensaje_resultado"));
    }

    private BusinessException missingOrConflict(UUID id, UUID empresa) {
        boolean exists = findById(id, empresa).isPresent();
        return new BusinessException(exists ? ErrorCode.MOVEMENT_VERSION_CONFLICT : ErrorCode.MOVEMENT_NOT_FOUND);
    }
}
