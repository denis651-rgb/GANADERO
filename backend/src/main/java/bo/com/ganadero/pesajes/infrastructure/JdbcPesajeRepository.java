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
            "select p.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "l.nombre as lote_nombre, pt.nombre as potrero_nombre, pr.nombre as propiedad_nombre, " +
            "concat(pu.nombres, ' ', pu.apellidos) as responsable_nombre";
    private static final String FROM_JOIN =
            " from produccion.pesajes p " +
            "left join ganado.animales a on a.id=p.animal_id " +
            "left join ganado.lotes_ganaderos l on l.id=p.lote_id " +
            "left join campo.potreros pt on pt.id=p.potrero_id " +
            "left join core.propiedades pr on pr.id=p.propiedad_id " +
            "left join seguridad.perfiles_usuario pu on pu.id=p.responsable_id";

    @Override
    public PesajePage findAll(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                              UUID animalId, UUID propiedadId, int page, int size) {
        StringBuilder filter = new StringBuilder(" where p.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todasPropiedades) {
            if (propiedades.isEmpty()) {
                filter.append(" and 1=0");
            } else {
                filter.append(" and p.propiedad_id in (:allowedProperties)");
                params.put("allowedProperties", propiedades);
            }
        }
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
                r.getString("lote_nombre"), r.getString("potrero_nombre"), r.getString("propiedad_nombre"),
                r.getString("responsable_nombre"), r.getLong("version"));
    }

    @Override
    public Optional<PesajeIndicadorLote> indicadorLote(UUID loteId, UUID empresa) {
        return jdbc.sql("""
                select l.id as lote_id, l.codigo as codigo_lote, l.nombre as nombre_lote,
                       count(distinct a.id) filter (where a.estado = 'ACTIVO') as animales_totales,
                       count(distinct u.animal_id) as animales_pesados,
                       round(avg(u.peso_kg), 2) as peso_promedio_kg,
                       min(u.peso_kg) as peso_minimo_kg,
                       max(u.peso_kg) as peso_maximo_kg,
                       min(u.fecha) as fecha_primer_pesaje,
                       max(u.fecha) as fecha_ultimo_pesaje
                from ganado.lotes_ganaderos l
                left join ganado.animales a on a.lote_actual_id = l.id and a.empresa_id = l.empresa_id
                left join produccion.v_ultimo_peso_animal u on u.animal_id = a.id and u.empresa_id = l.empresa_id
                where l.id = :lote and l.empresa_id = :e
                group by l.id, l.codigo, l.nombre
                """).param("lote", loteId).param("e", empresa).query(this::mapIndicadorLote).optional();
    }

    @Override
    public BigDecimal promedioPesoLote(UUID loteId, UUID empresa) {
        return jdbc.sql("""
                select round(avg(u.peso_kg), 2)
                from produccion.v_promedio_peso_lote u
                where u.lote_id = :lote and u.empresa_id = :e
                """).param("lote", loteId).param("e", empresa).query(BigDecimal.class).optional().orElse(null);
    }

    @Override
    public long countAnimalesActivosLote(UUID loteId, UUID empresa) {
        return jdbc.sql("""
                select count(*) from ganado.animales
                where empresa_id = :e and lote_actual_id = :lote and estado = 'ACTIVO'
                """).param("e", empresa).param("lote", loteId).query(Long.class).single();
    }

    @Override
    public List<PesajeSinPesaje> animalesSinPesaje(UUID empresa, boolean todasPropiedades, Set<UUID> propiedades,
                                                   int page, int size) {
        StringBuilder sql = new StringBuilder("""
                select id, codigo, nombre, ultimo_pesaje, peso_ultimo_kg, dias_sin_pesaje
                from produccion.v_animales_sin_pesaje
                where empresa_id = :e
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todasPropiedades) {
            sql.append(" and propiedad_actual_id in (:properties)");
            params.put("properties", propiedades.isEmpty() ? List.of(UUID.randomUUID()) : propiedades);
        }
        sql.append(" order by dias_sin_pesaje desc nulls first limit :limit offset :offset");
        params.put("limit", size);
        params.put("offset", page * size);
        return jdbc.sql(sql.toString()).params(params).query(this::mapSinPesaje).list();
    }

    @Override
    public long countAnimalesSinPesaje(UUID empresa, boolean todasPropiedades, Set<UUID> propiedades) {
        StringBuilder sql = new StringBuilder("""
                select count(*) from produccion.v_animales_sin_pesaje
                where empresa_id = :e
                """);
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todasPropiedades) {
            sql.append(" and propiedad_actual_id in (:properties)");
            params.put("properties", propiedades.isEmpty() ? List.of(UUID.randomUUID()) : propiedades);
        }
        return jdbc.sql(sql.toString()).params(params).query(Long.class).single();
    }

    private PesajeIndicadorLote mapIndicadorLote(ResultSet r, int row) throws SQLException {
        Integer totales = r.getObject("animales_totales", Integer.class);
        Integer pesados = r.getObject("animales_pesados", Integer.class);
        return new PesajeIndicadorLote(
                r.getObject("lote_id", UUID.class), r.getString("codigo_lote"), r.getString("nombre_lote"),
                totales, pesados, totales == null ? 0 : totales - (pesados == null ? 0 : pesados),
                r.getBigDecimal("peso_promedio_kg"), r.getBigDecimal("peso_minimo_kg"),
                r.getBigDecimal("peso_maximo_kg"), r.getObject("fecha_primer_pesaje", LocalDate.class),
                r.getObject("fecha_ultimo_pesaje", LocalDate.class));
    }

    private PesajeSinPesaje mapSinPesaje(ResultSet r, int row) throws SQLException {
        return new PesajeSinPesaje(
                r.getObject("id", UUID.class), r.getString("codigo"), r.getString("nombre"),
                r.getObject("ultimo_pesaje", LocalDate.class), r.getBigDecimal("peso_ultimo_kg"),
                r.getLong("dias_sin_pesaje"));
    }
}
