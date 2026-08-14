package bo.com.ganadero.movimientos.infrastructure;

import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.movimientos.domain.*;
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
        StringBuilder where = new StringBuilder(" where m.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (estado != null) { where.append(" and m.estado=:estado"); params.put("estado", estado.name()); }
        if (tipo != null) { where.append(" and m.tipo=:tipo"); params.put("tipo", tipo.name()); }
        long total = jdbc.sql("select count(*) from ganado.movimientos m" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<Movimiento> values = jdbc.sql("select m.* from ganado.movimientos m" + where
                        + " order by m.created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return MovimientoPage.of(values, page, size, total);
    }

    @Override
    public Optional<Movimiento> findById(UUID id, UUID empresa) {
        return jdbc.sql("select * from ganado.movimientos where id=:id and empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::map).optional();
    }

    @Override
    public Optional<Movimiento> findByIdForUpdate(UUID id, UUID empresa) {
        return jdbc.sql("select * from ganado.movimientos where id=:id and empresa_id=:e for update")
                .param("id", id).param("e", empresa).query(this::map).optional();
    }

    @Override
    public Optional<Movimiento> findByOriginal(UUID id, UUID empresa) {
        return jdbc.sql("select * from ganado.movimientos where movimiento_revertido_id=:id and empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::map).optional();
    }

    @Override
    public List<MovimientoDetalle> findDetalles(UUID movimientoId) {
        return jdbc.sql("select * from ganado.movimiento_detalles where movimiento_id=:m")
                .param("m", movimientoId).query(this::mapDetalle).list();
    }

    @Override
    public Movimiento create(Movimiento movimiento, List<MovimientoAnimal> animales, UUID actor) {
        jdbc.sql("""
                insert into ganado.movimientos(id,empresa_id,tipo,estado,fecha_movimiento,motivo,observacion,
                    origen_propiedad_id,origen_potrero_id,origen_lote_id,destino_propiedad_id,destino_potrero_id,
                    destino_lote_id,usuario_crea,created_by,updated_by)
                values(:id,:e,:tipo,:estado,:fecha,:motivo,:obs,:op,:opr,:ol,:dp,:dpotrero,:dl,:actor,:actor,:actor)""")
                .param("id", movimiento.id()).param("e", movimiento.empresaId())
                .param("tipo", movimiento.tipo().name()).param("estado", movimiento.estado().name())
                .param("fecha", movimiento.fechaMovimiento()).param("motivo", movimiento.motivo())
                .param("obs", movimiento.observacion())
                .param("op", movimiento.origenPropiedadId()).param("opr", movimiento.origenPotreroId())
                .param("ol", movimiento.origenLoteId()).param("dp", movimiento.destinoPropiedadId())
                .param("dpotrero", movimiento.destinoPotreroId()).param("dl", movimiento.destinoLoteId())
                .param("actor", actor).update();
        insertDetalles(movimiento, animales);
        return findById(movimiento.id(), movimiento.empresaId()).orElseThrow();
    }

    @Override
    public Movimiento saveConfirmed(Movimiento movimiento, List<MovimientoAnimal> animales, UUID actor) {
        jdbc.sql("""
                insert into ganado.movimientos(id,empresa_id,tipo,estado,fecha_movimiento,motivo,observacion,
                    origen_propiedad_id,origen_potrero_id,origen_lote_id,destino_propiedad_id,destino_potrero_id,
                    destino_lote_id,usuario_crea,usuario_confirma,fecha_confirmacion,movimiento_revertido_id,
                    created_by,updated_by)
                values(:id,:e,:tipo,'CONFIRMADO',:fecha,:motivo,:obs,:op,:opr,:ol,:dp,:dpotrero,:dl,:actor,:actor,
                    now(),:rev,:actor,:actor)""")
                .param("id", movimiento.id()).param("e", movimiento.empresaId())
                .param("tipo", movimiento.tipo().name())
                .param("fecha", movimiento.fechaMovimiento()).param("motivo", movimiento.motivo())
                .param("obs", movimiento.observacion())
                .param("op", movimiento.origenPropiedadId()).param("opr", movimiento.origenPotreroId())
                .param("ol", movimiento.origenLoteId()).param("dp", movimiento.destinoPropiedadId())
                .param("dpotrero", movimiento.destinoPotreroId()).param("dl", movimiento.destinoLoteId())
                .param("rev", movimiento.movimientoRevertidoId()).param("actor", actor).update();
        insertDetalles(movimiento, animales);
        return findById(movimiento.id(), movimiento.empresaId()).orElseThrow();
    }

    private void insertDetalles(Movimiento movimiento, List<MovimientoAnimal> animales) {
        for (MovimientoAnimal animal : animales) {
            jdbc.sql("insert into ganado.movimiento_detalles(id,movimiento_id,animal_id,empresa_id,animal_version_esperada,estado_antes,estado_despues) values(:id,:m,:animal,:e,:ave,:antes,:despues)")
                    .param("id", UUID.randomUUID()).param("m", movimiento.id()).param("animal", animal.animalId())
                    .param("e", movimiento.empresaId()).param("ave", animal.version())
                    .param("antes", "ACTIVO").param("despues", "ACTIVO").update();
        }
    }

    @Override
    public Movimiento confirm(UUID id, UUID empresa, long version, UUID actor) {
        int changed = jdbc.sql("""
                update ganado.movimientos set estado='CONFIRMADO',usuario_confirma=:actor,fecha_confirmacion=now(),
                    updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and empresa_id=:e and version=:version and estado='PENDIENTE'""")
                .param("actor", actor).param("id", id).param("e", empresa).param("version", version).update();
        if (changed == 0) throw missingOrConflict(id, empresa);
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public Movimiento annul(UUID id, UUID empresa, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update ganado.movimientos set estado='ANULADO',usuario_anula=:actor,fecha_anulacion=now(),
                    motivo_anulacion=:motivo,updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and empresa_id=:e and version=:version and estado='PENDIENTE'""")
                .param("actor", actor).param("id", id).param("e", empresa).param("version", version)
                .param("motivo", motivo).update();
        if (changed == 0) throw missingOrConflict(id, empresa);
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public Movimiento markReverted(UUID id, UUID empresa, UUID reversionId, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update ganado.movimientos set estado='REVERTIDO',usuario_revierte=:actor,fecha_reversion=now(),
                    motivo_reversion=:motivo,movimiento_reversion_id=:rev,updated_at=now(),updated_by=:actor,
                    version=version+1
                where id=:id and empresa_id=:e and version=:version and estado='CONFIRMADO'""")
                .param("actor", actor).param("id", id).param("e", empresa).param("version", version)
                .param("motivo", motivo).param("rev", reversionId).update();
        if (changed == 0) throw missingOrConflict(id, empresa);
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public void saveDetalleUbicaciones(UUID movimientoId, List<MovimientoDetalle> detalle) {
        for (MovimientoDetalle d : detalle) {
            jdbc.sql("""
                    update ganado.movimiento_detalles set animal_version_esperada=:ave,propiedad_antes=:pa,
                        potrero_antes=:pr,lote_antes=:la,propiedad_despues=:pd,potrero_despues=:prd,lote_despues=:ld,
                        estado_resultado=:er,mensaje_resultado=:mr
                    where id=:id""")
                    .param("ave", d.animalVersionEsperada())
                    .param("pa", d.propiedadAntes()).param("pr", d.potreroAntes()).param("la", d.loteAntes())
                    .param("pd", d.propiedadDespues()).param("prd", d.potreroDespues()).param("ld", d.loteDespues())
                    .param("er", d.estadoResultado()).param("mr", d.mensajeResultado())
                    .param("id", d.id()).update();
        }
    }

    private Movimiento map(ResultSet rs, int rowNum) throws SQLException {
        Instant confirmacion = rs.getTimestamp("fecha_confirmacion") == null ? null : rs.getTimestamp("fecha_confirmacion").toInstant();
        Instant anulacion = rs.getTimestamp("fecha_anulacion") == null ? null : rs.getTimestamp("fecha_anulacion").toInstant();
        Instant reversion = rs.getTimestamp("fecha_reversion") == null ? null : rs.getTimestamp("fecha_reversion").toInstant();
        return new Movimiento(
                rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                TipoMovimiento.valueOf(rs.getString("tipo")), EstadoMovimiento.valueOf(rs.getString("estado")),
                rs.getObject("fecha_movimiento", LocalDate.class), rs.getString("motivo"),
                rs.getString("observacion"),
                rs.getObject("origen_propiedad_id", UUID.class), rs.getObject("origen_potrero_id", UUID.class),
                rs.getObject("origen_lote_id", UUID.class), rs.getObject("destino_propiedad_id", UUID.class),
                rs.getObject("destino_potrero_id", UUID.class), rs.getObject("destino_lote_id", UUID.class),
                rs.getObject("usuario_crea", UUID.class), rs.getObject("usuario_confirma", UUID.class),
                rs.getObject("usuario_anula", UUID.class), confirmacion, anulacion,
                rs.getString("motivo_anulacion"), rs.getObject("usuario_revierte", UUID.class),
                reversion, rs.getString("motivo_reversion"),
                rs.getObject("movimiento_revertido_id", UUID.class),
                rs.getObject("movimiento_reversion_id", UUID.class),
                rs.getLong("version"));
    }

    private MovimientoDetalle mapDetalle(ResultSet rs, int rowNum) throws SQLException {
        String antes = rs.getString("estado_antes");
        String despues = rs.getString("estado_despues");
        return new MovimientoDetalle(
                rs.getObject("id", UUID.class), rs.getObject("movimiento_id", UUID.class),
                rs.getObject("animal_id", UUID.class), rs.getLong("animal_version_esperada"),
                antes == null ? null : EstadoAnimal.valueOf(antes),
                despues == null ? null : EstadoAnimal.valueOf(despues),
                rs.getObject("propiedad_antes", UUID.class), rs.getObject("potrero_antes", UUID.class),
                rs.getObject("lote_antes", UUID.class), rs.getObject("propiedad_despues", UUID.class),
                rs.getObject("potrero_despues", UUID.class), rs.getObject("lote_despues", UUID.class),
                rs.getString("estado_resultado"), rs.getString("mensaje_resultado"));
    }

    private BusinessException missingOrConflict(UUID id, UUID empresa) {
        boolean exists = findById(id, empresa).isPresent();
        return new BusinessException(exists ? ErrorCode.MOVEMENT_VERSION_CONFLICT : ErrorCode.MOVEMENT_NOT_FOUND);
    }
}
