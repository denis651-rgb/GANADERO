package bo.com.ganadero.lotes.infrastructure;

import bo.com.ganadero.lotes.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
class JdbcLoteRepository implements LoteRepository {
    private final JdbcClient jdbc;

    JdbcLoteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LotePage findAll(UUID empresa, Set<UUID> propiedades, boolean todas, EstadoLote estado,
                            String search, int page, int size) {
        if (!todas && propiedades.isEmpty()) return LotePage.of(List.of(), page, size, 0);
        StringBuilder where = new StringBuilder(" where l.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
        if (!todas) {
            where.append(" and l.propiedad_id in (:propiedades)");
            params.put("propiedades", new ArrayList<>(propiedades));
        }
        if (estado != null) {
            where.append(" and l.estado=:estado");
            params.put("estado", estado.name());
        }
        if (search != null && !search.isBlank()) {
            where.append(" and (lower(l.codigo) like :search or lower(l.nombre) like :search)");
            params.put("search", "%" + search.toLowerCase() + "%");
        }
        long total = jdbc.sql("select count(*) from ganado.lotes_ganaderos l" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<Lote> values = jdbc.sql("select l.* from ganado.lotes_ganaderos l" + where
                        + " order by l.fecha_apertura desc, l.codigo limit :limit offset :offset")
                .params(params).query(this::map).list();
        return LotePage.of(values, page, size, total);
    }

    @Override
    public Optional<Lote> findById(UUID id, UUID empresa) {
        return jdbc.sql("select * from ganado.lotes_ganaderos where id=:id and empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::map).optional();
    }

    @Override
    public Lote create(Lote lote, UUID actor) {
        try {
            jdbc.sql("""
                    insert into ganado.lotes_ganaderos(id,empresa_id,propiedad_id,codigo,nombre,descripcion,
                        estado,fecha_apertura,created_by,updated_by)
                    values(:id,:e,:propiedad,:codigo,:nombre,:descripcion,:estado,:apertura,:actor,:actor)""")
                    .param("id", lote.id()).param("e", lote.empresaId()).param("propiedad", lote.propiedadId())
                    .param("codigo", lote.codigo()).param("nombre", lote.nombre()).param("descripcion", lote.descripcion())
                    .param("estado", lote.estado().name()).param("apertura", lote.fechaApertura()).param("actor", actor)
                    .update();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.LOT_CODE_ALREADY_EXISTS);
        }
        return findById(lote.id(), lote.empresaId()).orElseThrow();
    }

    @Override
    public Lote update(Lote lote, UUID actor) {
        try {
            int changed = jdbc.sql("""
                    update ganado.lotes_ganaderos set codigo=:codigo,nombre=:nombre,descripcion=:descripcion,
                        propiedad_id=:propiedad,updated_at=now(),updated_by=:actor,version=version+1
                    where id=:id and empresa_id=:e and version=:version""")
                    .param("codigo", lote.codigo()).param("nombre", lote.nombre()).param("descripcion", lote.descripcion())
                    .param("propiedad", lote.propiedadId()).param("actor", actor).param("id", lote.id())
                    .param("e", lote.empresaId()).param("version", lote.version()).update();
            if (changed == 0) throw missingOrConflict(findById(lote.id(), lote.empresaId()).isPresent());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.LOT_CODE_ALREADY_EXISTS);
        }
        return findById(lote.id(), lote.empresaId()).orElseThrow();
    }

    @Override
    public Lote close(UUID id, UUID empresa, long version, LocalDate fechaCierre, String motivoCierre, UUID actor) {
        int changed = jdbc.sql("""
                update ganado.lotes_ganaderos set estado='CERRADO',fecha_cierre=coalesce(:fecha,current_date),
                    motivo_cierre=:motivo,updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and empresa_id=:e and version=:version and estado='ACTIVO'""")
                .param("fecha", fechaCierre).param("motivo", motivoCierre).param("actor", actor)
                .param("id", id).param("e", empresa).param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(id, empresa).isPresent());
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public List<MembresiaLote> findMemberships(UUID loteId, UUID empresa, boolean soloActivas) {
        String sql = "select m.* from ganado.membresias_lote m where m.lote_id=:lote and m.empresa_id=:e";
        if (soloActivas) sql += " and m.fecha_salida is null";
        sql += " order by m.fecha_ingreso desc";
        return jdbc.sql(sql).param("lote", loteId).param("e", empresa).query(this::mapMembership).list();
    }

    @Override
    public Optional<Lote> findActiveLotOfAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql("""
                select l.* from ganado.lotes_ganaderos l
                join ganado.membresias_lote m on m.lote_id=l.id
                where m.animal_id=:animal and m.empresa_id=:e and m.fecha_salida is null and l.estado='ACTIVO' limit 1""")
                .param("animal", animalId).param("e", empresa).query(this::map).optional();
    }

    @Override
    public boolean hasActiveAnimals(UUID loteId, UUID empresa) {
        return jdbc.sql("select exists(select 1 from ganado.membresias_lote where lote_id=:lote and empresa_id=:e and fecha_salida is null)")
                .param("lote", loteId).param("e", empresa).query(Boolean.class).single();
    }

    @Override
    public Optional<MembresiaLote> findActiveMembership(UUID animalId, UUID empresa) {
        return jdbc.sql("select m.* from ganado.membresias_lote m " +
                        "where m.animal_id=:animal and m.empresa_id=:e and m.fecha_salida is null for update")
                .param("animal", animalId).param("e", empresa).query(this::mapMembership).optional();
    }

    @Override
    public MembresiaLote openMembership(UUID loteId, String loteCodigo, UUID animalId, UUID empresa,
                                        String motivoIngreso, String observacion, String modo,
                                        Instant fechaIngreso, UUID actor) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.sql("""
                    insert into ganado.membresias_lote(id,empresa_id,lote_id,animal_id,fecha_ingreso,
                        motivo_ingreso,observacion,modo,ingresado_por)
                    values(:id,:e,:lote,:animal,:ingreso,:motivo,:obs,:modo,:actor)""")
                    .param("id", id).param("e", empresa).param("lote", loteId).param("animal", animalId)
                    .param("ingreso", Timestamp.from(fechaIngreso)).param("motivo", motivoIngreso)
                    .param("obs", observacion).param("modo", modo).param("actor", actor).update();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.ANIMAL_ALREADY_IN_LOT);
        }
        return jdbc.sql("select m.* from ganado.membresias_lote m where m.id=:id").param("id", id)
                .query(this::mapMembership).single();
    }

    @Override
    public void closeMembership(UUID loteId, String loteCodigo, UUID animalId, UUID empresa,
                                String motivo, Instant fechaSalida, UUID actor) {
        jdbc.sql("""
                update ganado.membresias_lote set fecha_salida=:salida,motivo_salida=:motivo,salida_por=:actor,
                    version=version+1
                where lote_id=:lote and animal_id=:animal and empresa_id=:e and fecha_salida is null""")
                .param("salida", Timestamp.from(fechaSalida)).param("motivo", motivo).param("actor", actor)
                .param("lote", loteId).param("animal", animalId).param("e", empresa).update();
    }

    @Override
    public MembresiaLotePage findHistory(UUID loteId, UUID empresa, UUID animalId, Instant desde, Instant hasta,
                                         String motivoIngreso, String motivoSalida, int page, int size) {
        StringBuilder where = new StringBuilder(" where m.lote_id=:lote and m.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("lote", loteId);
        params.put("e", empresa);
        if (animalId != null) {
            where.append(" and m.animal_id=:animal");
            params.put("animal", animalId);
        }
        if (desde != null) {
            where.append(" and m.fecha_ingreso >= :desde");
            params.put("desde", Timestamp.from(desde));
        }
        if (hasta != null) {
            where.append(" and m.fecha_ingreso <= :hasta");
            params.put("hasta", Timestamp.from(hasta));
        }
        if (motivoIngreso != null && !motivoIngreso.isBlank()) {
            where.append(" and lower(coalesce(m.motivo_ingreso,'')) like :mi");
            params.put("mi", "%" + motivoIngreso.toLowerCase() + "%");
        }
        if (motivoSalida != null && !motivoSalida.isBlank()) {
            where.append(" and lower(coalesce(m.motivo_salida,'')) like :ms");
            params.put("ms", "%" + motivoSalida.toLowerCase() + "%");
        }
        long total = jdbc.sql("select count(*) from ganado.membresias_lote m" + where).params(params)
                .query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<MembresiaLote> values = jdbc.sql("select m.* from ganado.membresias_lote m" + where
                        + " order by m.fecha_ingreso desc limit :limit offset :offset")
                .params(params).query(this::mapMembership).list();
        return MembresiaLotePage.of(values, page, size, total);
    }

    private Lote map(ResultSet rs, int rowNum) throws SQLException {
        return new Lote(rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                rs.getObject("propiedad_id", UUID.class), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("descripcion"), EstadoLote.valueOf(rs.getString("estado")),
                rs.getObject("fecha_apertura", LocalDate.class), rs.getObject("fecha_cierre", LocalDate.class),
                rs.getLong("version"));
    }

    private MembresiaLote mapMembership(ResultSet rs, int rowNum) throws SQLException {
        Instant salida = rs.getTimestamp("fecha_salida") == null ? null : rs.getTimestamp("fecha_salida").toInstant();
        return new MembresiaLote(rs.getObject("id", UUID.class), rs.getObject("lote_id", UUID.class),
                rs.getObject("animal_id", UUID.class), rs.getTimestamp("fecha_ingreso").toInstant(), salida,
                rs.getString("motivo_ingreso"), rs.getString("motivo_salida"), rs.getString("observacion"),
                rs.getString("modo"), rs.getObject("ingresado_por", UUID.class),
                rs.getObject("salida_por", UUID.class), rs.getLong("version"));
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.LOT_VERSION_CONFLICT : ErrorCode.LOT_NOT_FOUND);
    }
}
