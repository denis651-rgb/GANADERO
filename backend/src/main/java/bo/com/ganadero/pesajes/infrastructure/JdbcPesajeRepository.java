package bo.com.ganadero.pesajes.infrastructure;

import bo.com.ganadero.pesajes.domain.*;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

@Repository
public class JdbcPesajeRepository implements PesajeRepository {
    private final JdbcClient jdbc;

    public JdbcPesajeRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_COLUMNS =
            "select p.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "l.nombre as lote_nombre, pt.nombre as potrero_nombre";
    private static final String FROM_JOIN =
            " from pesaje p " +
            "left join animal a on a.id=p.animal_id " +
            "left join lote_ganadero l on l.id=p.lote_id " +
            "left join potrero pt on pt.id=p.potrero_id";

    @Override
    public PesajePage findAll(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                              UUID animalId, UUID propiedadId, int page, int size) {
        StringBuilder filter = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (animalId != null) {
            filter.append(" and p.animal_id=:animal");
            params.put("animal", animalId.toString());
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
        return jdbc.sql(SELECT_COLUMNS + FROM_JOIN + " where p.id=:id")
                .param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public Optional<Pesaje> findByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_COLUMNS + FROM_JOIN + " where p.cliente_uuid=:cliente")
                .param("cliente", clienteUuid.toString()).query(this::map).optional();
    }

    @Override
    public List<Pesaje> findByAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_COLUMNS + FROM_JOIN + " where p.animal_id=:animal"
                + " order by p.fecha desc, p.created_at desc")
                .param("animal", animalId.toString()).query(this::map).list();
    }

    @Override
    public List<UUID> listActiveAnimalsOfLote(UUID loteId, UUID empresa) {
        return jdbc.sql("select id from animal where lote_actual_id=:lote and estado='ACTIVO'")
                .param("lote", loteId.toString()).query(String.class).list().stream().map(UUID::fromString).toList();
    }

    @Override
    public Pesaje create(Pesaje p, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into pesaje(id,animal_id,fecha,peso_kg,tipo,condicion_corporal,bascula,
                    responsable_id,potrero_id,lote_id,dispositivo,cliente_uuid,idempotency_key,estado,observaciones,
                    created_by,updated_by)
                    values(:id,:animal,:fecha,:peso,:tipo,:condicion,:bascula,:responsable,:potrero,:lote,
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
                update pesaje set estado='ANULADO',motivo_anulacion=:motivo,anulado_por=:actor,
                fecha_anulacion=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='ACTIVO'
                """).param("motivo", motivo).param("actor", actor.toString()).param("id", id.toString())
                .param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(id, empresa).isPresent());
        return findById(id, empresa).orElseThrow();
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.VERSION_CONFLICT : ErrorCode.PESAJE_NOT_FOUND);
    }

    private Map<String, Object> params(Pesaje p, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.id().toString());
        map.put("animal", p.animalId().toString());
        map.put("fecha", p.fecha() == null ? null : p.fecha().toString());
        map.put("peso", p.pesoKg());
        map.put("tipo", p.tipo().name());
        map.put("condicion", p.condicionCorporal());
        map.put("bascula", p.bascula());
        map.put("responsable", p.responsableId() == null ? null : p.responsableId().toString());
        map.put("potrero", p.potreroId() == null ? null : p.potreroId().toString());
        map.put("lote", p.loteId() == null ? null : p.loteId().toString());
        map.put("dispositivo", p.dispositivo());
        map.put("cliente", p.clienteUuid() == null ? null : p.clienteUuid().toString());
        map.put("idempotency", p.idempotencyKey());
        map.put("estado", p.estado().name());
        map.put("observaciones", p.observaciones());
        map.put("actor", actor.toString());
        return map;
    }

    private Pesaje map(ResultSet r, int row) throws SQLException {
        String estado = r.getString("estado");
        String tipo = r.getString("tipo");
        return new Pesaje(Rows.uuid(r, "id"), null,
                Rows.uuid(r, "animal_id"), r.getString("fecha") == null ? null : LocalDate.parse(r.getString("fecha")),
                r.getBigDecimal("peso_kg"), tipo == null ? null : TipoPesaje.valueOf(tipo),
                r.getBigDecimal("condicion_corporal"), r.getString("bascula"),
                Rows.uuid(r, "responsable_id"), null,
                Rows.uuid(r, "potrero_id"), Rows.uuid(r, "lote_id"),
                r.getString("dispositivo"), Rows.uuid(r, "cliente_uuid"),
                r.getString("idempotency_key"), estado == null ? null : EstadoPesaje.valueOf(estado),
                r.getString("motivo_anulacion"), Rows.uuid(r, "anulado_por"),
                Rows.instant(r, "fecha_anulacion"),
                r.getString("observaciones"), r.getString("animal_codigo"), r.getString("animal_nombre"),
                r.getString("lote_nombre"), r.getString("potrero_nombre"), null,
                null, r.getLong("version"));
    }

    @Override
    public Optional<PesajeIndicadorLote> indicadorLote(UUID loteId, UUID empresa) {
        return jdbc.sql("""
                select l.id as lote_id, l.codigo as codigo_lote, l.nombre as nombre_lote,
                       (select count(*) from animal a where a.lote_actual_id=l.id and a.estado='ACTIVO') as animales_totales,
                       (select count(distinct p.animal_id) from pesaje p join animal a on a.id=p.animal_id
                            where a.lote_actual_id=l.id and p.estado='ACTIVO') as animales_pesados,
                       round((select avg(p.peso_kg) from pesaje p join animal a on a.id=p.animal_id
                            where a.lote_actual_id=l.id and p.estado='ACTIVO'), 2) as peso_promedio_kg,
                       (select min(p.peso_kg) from pesaje p join animal a on a.id=p.animal_id
                            where a.lote_actual_id=l.id and p.estado='ACTIVO') as peso_minimo_kg,
                       (select max(p.peso_kg) from pesaje p join animal a on a.id=p.animal_id
                            where a.lote_actual_id=l.id and p.estado='ACTIVO') as peso_maximo_kg,
                       (select min(p.fecha) from pesaje p join animal a on a.id=p.animal_id
                            where a.lote_actual_id=l.id and p.estado='ACTIVO') as fecha_primer_pesaje,
                       (select max(p.fecha) from pesaje p join animal a on a.id=p.animal_id
                            where a.lote_actual_id=l.id and p.estado='ACTIVO') as fecha_ultimo_pesaje
                from lote_ganadero l
                where l.id = :lote
                """).param("lote", loteId.toString()).query(this::mapIndicadorLote).optional();
    }

    @Override
    public BigDecimal promedioPesoLote(UUID loteId, UUID empresa) {
        return jdbc.sql("""
                select round(avg(p.peso_kg), 2) from pesaje p join animal a on a.id=p.animal_id
                where a.lote_actual_id=:lote and p.estado='ACTIVO'
                """).param("lote", loteId.toString()).query(BigDecimal.class).optional().orElse(null);
    }

    @Override
    public long countAnimalesActivosLote(UUID loteId, UUID empresa) {
        return jdbc.sql("""
                select count(*) from animal
                where lote_actual_id = :lote and estado = 'ACTIVO'
                """).param("lote", loteId.toString()).query(Long.class).single();
    }

    @Override
    public List<PesajeSinPesaje> animalesSinPesaje(UUID empresa, boolean todasPropiedades, Set<UUID> propiedades,
                                                   int page, int size) {
        String sql = """
                select a.id, a.codigo, a.nombre,
                       (select max(p.fecha) from pesaje p where p.animal_id=a.id and p.estado='ACTIVO') as ultimo_pesaje,
                       (select p.peso_kg from pesaje p where p.animal_id=a.id and p.estado='ACTIVO' order by p.fecha desc limit 1) as peso_ultimo_kg,
                       cast(julianday('now') - julianday(coalesce(
                           (select max(p.fecha) from pesaje p where p.animal_id=a.id and p.estado='ACTIVO'), a.created_at)) as integer) as dias_sin_pesaje
                from animal a
                where a.estado='ACTIVO'
                order by dias_sin_pesaje desc limit :limit offset :offset
                """;
        return jdbc.sql(sql).param("limit", size).param("offset", page * size).query(this::mapSinPesaje).list();
    }

    @Override
    public long countAnimalesSinPesaje(UUID empresa, boolean todasPropiedades, Set<UUID> propiedades) {
        return jdbc.sql("select count(*) from animal where estado='ACTIVO'").query(Long.class).single();
    }

    private PesajeIndicadorLote mapIndicadorLote(ResultSet r, int row) throws SQLException {
        Integer totales = r.getObject("animales_totales", Integer.class);
        Integer pesados = r.getObject("animales_pesados", Integer.class);
        return new PesajeIndicadorLote(
                Rows.uuid(r, "lote_id"), r.getString("codigo_lote"), r.getString("nombre_lote"),
                totales, pesados, totales == null ? 0 : totales - (pesados == null ? 0 : pesados),
                r.getBigDecimal("peso_promedio_kg"), r.getBigDecimal("peso_minimo_kg"),
                r.getBigDecimal("peso_maximo_kg"),
                r.getString("fecha_primer_pesaje") == null ? null : LocalDate.parse(r.getString("fecha_primer_pesaje")),
                r.getString("fecha_ultimo_pesaje") == null ? null : LocalDate.parse(r.getString("fecha_ultimo_pesaje")));
    }

    private PesajeSinPesaje mapSinPesaje(ResultSet r, int row) throws SQLException {
        return new PesajeSinPesaje(
                Rows.uuid(r, "id"), r.getString("codigo"), r.getString("nombre"),
                r.getString("ultimo_pesaje") == null ? null : LocalDate.parse(r.getString("ultimo_pesaje")),
                r.getBigDecimal("peso_ultimo_kg"),
                r.getLong("dias_sin_pesaje"));
    }
}
