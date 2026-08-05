package bo.com.ganadero.pesajes.infrastructure;

import bo.com.ganadero.pesajes.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Repository
public class JdbcPesajeRepository implements PesajeRepository {
    private final JdbcClient jdbc;

    public JdbcPesajeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLUMNS =
            "select p.*, a.codigo as animal_codigo, a.nombre as animal_nombre";
    private static final String FROM_JOIN =
            " from produccion.pesajes p left join ganado.animales a on a.id=p.animal_id";

    @Override
    public PesajePage findAll(UUID empresa, UUID animalId, UUID propiedadId, int page, int size) {
        StringBuilder filter = new StringBuilder(" where p.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (animalId != null) {
            filter.append(" and p.animal_id=:animal");
            params.put("animal", animalId);
        }
        if (propiedadId != null) {
            filter.append(" and p.propiedad_id=:property");
            params.put("property", propiedadId);
        }
        long total = jdbc.sql("select count(*)" + FROM_JOIN + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<Pesaje> content = jdbc.sql(SELECT_COLUMNS + FROM_JOIN + filter
                + " order by p.fecha desc, p.created_at desc limit :limit offset :offset")
                .params(params).query(this::map).list();
        return PesajePage.of(content, page, size, total);
    }

    @Override
    public Optional<Pesaje> findById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_COLUMNS + FROM_JOIN + " where p.id=:id and p.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::map).optional();
    }

    @Override
    public Optional<Pesaje> findByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_COLUMNS + FROM_JOIN + " where p.cliente_uuid=:cliente and p.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::map).optional();
    }

    @Override
    public List<Pesaje> findByAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_COLUMNS + FROM_JOIN + " where p.animal_id=:animal and p.empresa_id=:e"
                + " order by p.fecha desc, p.created_at desc")
                .param("animal", animalId).param("e", empresa).query(this::map).list();
    }

    @Override
    public List<UUID> listActiveAnimalsOfLote(UUID loteId, UUID empresa) {
        return jdbc.sql("select id from ganado.animales where empresa_id=:e and lote_actual_id=:lote and estado='ACTIVO'")
                .param("e", empresa).param("lote", loteId).query(UUID.class).list();
    }

    @Override
    public Pesaje create(Pesaje p, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into produccion.pesajes(id,empresa_id,animal_id,fecha,peso_kg,tipo,condicion_corporal,bascula,
                    responsable_id,propiedad_id,potrero_id,lote_id,dispositivo,cliente_uuid,idempotency_key,estado,observaciones,
                    created_by,updated_by)
                    values(:id,:e,:animal,:fecha,:peso,:tipo,:condicion,:bascula,:responsable,:propiedad,:potrero,:lote,
                    :dispositivo,:cliente,:idempotency,:estado,:observaciones,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(params(p, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (p.clienteUuid() != null) {
                return findByClienteUuid(p.clienteUuid(), p.empresaId())
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (p.clienteUuid() != null) {
                return findByClienteUuid(p.clienteUuid(), p.empresaId())
                        .orElseGet(() -> findById(p.id(), p.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.PESAJE_DUPLICATED)));
            }
            return findById(p.id(), p.empresaId()).orElseThrow(() -> new BusinessException(ErrorCode.PESAJE_DUPLICATED));
        }
        return findById(p.id(), p.empresaId()).orElseThrow();
    }

    @Override
    public Pesaje annul(UUID id, UUID empresa, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update produccion.pesajes set estado='ANULADO',motivo_anulacion=:motivo,anulado_por=:actor,
                fecha_anulacion=now(),updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and empresa_id=:e and version=:version and estado='ACTIVO'
                """).param("motivo", motivo).param("actor", actor).param("id", id).param("e", empresa)
                .param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(id, empresa).isPresent());
        return findById(id, empresa).orElseThrow();
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.VERSION_CONFLICT : ErrorCode.PESAJE_NOT_FOUND);
    }

    private Map<String, Object> params(Pesaje p, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.id());
        map.put("e", p.empresaId());
        map.put("animal", p.animalId());
        map.put("fecha", p.fecha());
        map.put("peso", p.pesoKg());
        map.put("tipo", p.tipo().name());
        map.put("condicion", p.condicionCorporal());
        map.put("bascula", p.bascula());
        map.put("responsable", p.responsableId());
        map.put("propiedad", p.propiedadId());
        map.put("potrero", p.potreroId());
        map.put("lote", p.loteId());
        map.put("dispositivo", p.dispositivo());
        map.put("cliente", p.clienteUuid());
        map.put("idempotency", p.idempotencyKey());
        map.put("estado", p.estado().name());
        map.put("observaciones", p.observaciones());
        map.put("actor", actor);
        return map;
    }

    private Pesaje map(ResultSet r, int row) throws SQLException {
        String estado = r.getString("estado");
        String tipo = r.getString("tipo");
        return new Pesaje(r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("animal_id", UUID.class), r.getObject("fecha", LocalDate.class),
                r.getBigDecimal("peso_kg"), tipo == null ? null : TipoPesaje.valueOf(tipo),
                r.getBigDecimal("condicion_corporal"), r.getString("bascula"),
                r.getObject("responsable_id", UUID.class), r.getObject("propiedad_id", UUID.class),
                r.getObject("potrero_id", UUID.class), r.getObject("lote_id", UUID.class),
                r.getString("dispositivo"), r.getObject("cliente_uuid", UUID.class),
                r.getString("idempotency_key"), estado == null ? null : EstadoPesaje.valueOf(estado),
                r.getString("motivo_anulacion"), r.getObject("anulado_por", UUID.class),
                r.getTimestamp("fecha_anulacion") == null ? null : r.getTimestamp("fecha_anulacion").toInstant(),
                r.getString("observaciones"), r.getString("animal_codigo"), r.getString("animal_nombre"),
                r.getLong("version"));
    }
}
