package bo.com.ganadero.lotes.infrastructure;

import bo.com.ganadero.lotes.domain.*;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessException;
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
import java.util.Set;
import java.util.UUID;

@Repository
class JdbcLoteRepository implements LoteRepository {
    private final JdbcClient jdbc;
    private static final String SELECT = "select l.*, (select count(*) from membresia_lote m where m.lote_id=l.id and m.fecha_salida is null) as cantidad_actual from lote_ganadero l";

    JdbcLoteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LotePage findAll(UUID empresa, Set<UUID> propiedades, boolean todas, EstadoLote estado,
                            String search, int page, int size) {
        StringBuilder where = new StringBuilder(" where 1=1");
        Map<String, Object> params = new HashMap<>();
        if (estado != null) {
            where.append(" and l.estado=:estado");
            params.put("estado", estado.name());
        }
        if (search != null && !search.isBlank()) {
            where.append(" and (lower(l.codigo) like :search or lower(l.nombre) like :search)");
            params.put("search", "%" + search.toLowerCase() + "%");
        }
        long total = jdbc.sql("select count(*) from lote_ganadero l" + where).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<Lote> values = jdbc.sql(SELECT + where
                        + " order by l.fecha_apertura desc, l.codigo limit :limit offset :offset")
                .params(params).query(this::map).list();
        return LotePage.of(values, page, size, total);
    }

    @Override
    public Optional<Lote> findById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT + " where l.id=:id")
                .param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public Lote create(Lote lote, UUID actor) {
        try {
            jdbc.sql("""
                    insert into lote_ganadero(id,propiedad_id,codigo,nombre,descripcion,
                        estado,fecha_apertura,cantidad_maxima,created_by,updated_by)
                    values(:id,:propiedad,:codigo,:nombre,:descripcion,:estado,:apertura,:maximo,:actor,:actor)""")
                    .param("id", lote.id().toString())
                    .param("propiedad", lote.propiedadId().toString())
                    .param("codigo", lote.codigo()).param("nombre", lote.nombre()).param("descripcion", lote.descripcion())
                    .param("maximo", lote.cantidadMaxima())
                    .param("estado", lote.estado().name()).param("apertura", lote.fechaApertura() == null ? null : lote.fechaApertura().toString())
                    .param("actor", actor.toString())
                    .update();
        } catch (DataAccessException ex) {
            if (ex.getMostSpecificCause().getMessage().contains("LOT_CAPACITY_EXCEEDED")) throw new BusinessException(ErrorCode.LOT_CAPACITY_EXCEEDED);
            if (!(ex instanceof DataIntegrityViolationException)) throw ex;
            throw new BusinessException(ErrorCode.LOT_CODE_ALREADY_EXISTS);
        }
        return findById(lote.id(), lote.empresaId()).orElseThrow();
    }

    @Override
    public Lote update(Lote lote, UUID actor) {
        try {
            int changed = jdbc.sql("""
                    update lote_ganadero set propiedad_id=coalesce(:propiedad,propiedad_id),codigo=:codigo,nombre=:nombre,descripcion=:descripcion,cantidad_maxima=:maximo,
                        updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                    where id=:id and version=:version""")
                    .param("propiedad", lote.propiedadId() == null ? null : lote.propiedadId().toString())
                    .param("codigo", lote.codigo()).param("nombre", lote.nombre()).param("descripcion", lote.descripcion())
                    .param("actor", actor.toString()).param("id", lote.id().toString())
                    .param("maximo", lote.cantidadMaxima()).param("version", lote.version()).update();
            if (changed == 0) throw missingOrConflict(findById(lote.id(), lote.empresaId()).isPresent());
        } catch (DataAccessException ex) {
            if (ex.getMostSpecificCause().getMessage().contains("LOT_CAPACITY_EXCEEDED")) throw new BusinessException(ErrorCode.LOT_CAPACITY_EXCEEDED);
            if (!(ex instanceof DataIntegrityViolationException)) throw ex;
            throw new BusinessException(ErrorCode.LOT_CODE_ALREADY_EXISTS);
        }
        return findById(lote.id(), lote.empresaId()).orElseThrow();
    }

    @Override
    public Lote close(UUID id, UUID empresa, long version, LocalDate fechaCierre, String motivoCierre, UUID actor) {
        int changed = jdbc.sql("""
                update lote_ganadero set estado='CERRADO',fecha_cierre=coalesce(:fecha,date('now')),
                    motivo_cierre=:motivo,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and version=:version and estado='ACTIVO'""")
                .param("fecha", fechaCierre == null ? null : fechaCierre.toString()).param("motivo", motivoCierre).param("actor", actor.toString())
                .param("id", id.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(id, empresa).isPresent());
        return findById(id, empresa).orElseThrow();
    }

    @Override
    public List<MembresiaLote> findMemberships(UUID loteId, UUID empresa, boolean soloActivas) {
        String sql = "select m.*, (select a.codigo from animal a where a.id=m.animal_id) as animal_codigo, (select a.nombre from animal a where a.id=m.animal_id) as animal_nombre from membresia_lote m where m.lote_id=:lote";
        if (soloActivas) sql += " and m.fecha_salida is null";
        sql += " order by m.fecha_ingreso desc";
        return jdbc.sql(sql).param("lote", loteId.toString()).query(this::mapMembership).list();
    }

    @Override
    public Optional<Lote> findActiveLotOfAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT + " join membresia_lote m on m.lote_id=l.id where m.animal_id=:animal and m.fecha_salida is null and l.estado='ACTIVO' limit 1")
                .param("animal", animalId.toString()).query(this::map).optional();
    }

    @Override
    public boolean hasActiveAnimals(UUID loteId, UUID empresa) {
        return jdbc.sql("select exists(select 1 from membresia_lote where lote_id=:lote and fecha_salida is null)")
                .param("lote", loteId.toString()).query(Boolean.class).single();
    }

    @Override
    public Optional<MembresiaLote> findActiveMembership(UUID animalId, UUID empresa) {
        return jdbc.sql("select m.*, (select a.codigo from animal a where a.id=m.animal_id) as animal_codigo, (select a.nombre from animal a where a.id=m.animal_id) as animal_nombre from membresia_lote m " +
                        "where m.animal_id=:animal and m.fecha_salida is null")
                .param("animal", animalId.toString()).query(this::mapMembership).optional();
    }

    @Override
    public MembresiaLote openMembership(UUID loteId, String loteCodigo, UUID animalId, UUID empresa,
                                        String motivoIngreso, String observacion, String modo,
                                        Instant fechaIngreso, UUID actor) {
        UUID id = UUID.randomUUID();
        try {
            jdbc.sql("""
                    insert into membresia_lote(id,lote_id,animal_id,fecha_ingreso,
                        motivo_ingreso,observacion,modo,ingresado_por)
                    values(:id,:lote,:animal,:ingreso,:motivo,:obs,:modo,:actor)""")
                    .param("id", id.toString()).param("lote", loteId.toString()).param("animal", animalId.toString())
                    .param("ingreso", fechaIngreso.toString()).param("motivo", motivoIngreso)
                    .param("obs", observacion).param("modo", modo).param("actor", actor.toString()).update();
        } catch (DataAccessException ex) {
            if (ex.getMostSpecificCause().getMessage().contains("LOT_CAPACITY_EXCEEDED")) throw new BusinessException(ErrorCode.LOT_CAPACITY_EXCEEDED);
            if (!(ex instanceof DataIntegrityViolationException)) throw ex;
            throw new BusinessException(ErrorCode.ANIMAL_ALREADY_IN_LOT);
        }
        return jdbc.sql("select m.*, (select a.codigo from animal a where a.id=m.animal_id) as animal_codigo, (select a.nombre from animal a where a.id=m.animal_id) as animal_nombre from membresia_lote m where m.id=:id").param("id", id.toString())
                .query(this::mapMembership).single();
    }

    @Override
    public void closeMembership(UUID loteId, String loteCodigo, UUID animalId, UUID empresa,
                                String motivo, Instant fechaSalida, UUID actor) {
        jdbc.sql("""
                update membresia_lote set fecha_salida=:salida,motivo_salida=:motivo,salida_por=:actor,
                    version=version+1
                where lote_id=:lote and animal_id=:animal and fecha_salida is null""")
                .param("salida", fechaSalida.toString()).param("motivo", motivo).param("actor", actor.toString())
                .param("lote", loteId.toString()).param("animal", animalId.toString()).update();
    }

    @Override
    public MembresiaLotePage findHistory(UUID loteId, UUID empresa, UUID animalId, Instant desde, Instant hasta,
                                         String motivoIngreso, String motivoSalida, int page, int size) {
        StringBuilder where = new StringBuilder(" where m.lote_id=:lote");
        Map<String, Object> params = new HashMap<>();
        params.put("lote", loteId.toString());
        if (animalId != null) {
            where.append(" and m.animal_id=:animal");
            params.put("animal", animalId.toString());
        }
        if (desde != null) {
            where.append(" and m.fecha_ingreso >= :desde");
            params.put("desde", desde.toString());
        }
        if (hasta != null) {
            where.append(" and m.fecha_ingreso <= :hasta");
            params.put("hasta", hasta.toString());
        }
        if (motivoIngreso != null && !motivoIngreso.isBlank()) {
            where.append(" and lower(coalesce(m.motivo_ingreso,'')) like :mi");
            params.put("mi", "%" + motivoIngreso.toLowerCase() + "%");
        }
        if (motivoSalida != null && !motivoSalida.isBlank()) {
            where.append(" and lower(coalesce(m.motivo_salida,'')) like :ms");
            params.put("ms", "%" + motivoSalida.toLowerCase() + "%");
        }
        long total = jdbc.sql("select count(*) from membresia_lote m" + where).params(params)
                .query(Long.class).single();
        params.put("limit", size);
        params.put("offset", (long) page * size);
        List<MembresiaLote> values = jdbc.sql("select m.*, (select a.codigo from animal a where a.id=m.animal_id) as animal_codigo, (select a.nombre from animal a where a.id=m.animal_id) as animal_nombre from membresia_lote m" + where
                        + " order by m.fecha_ingreso desc limit :limit offset :offset")
                .params(params).query(this::mapMembership).list();
        return MembresiaLotePage.of(values, page, size, total);
    }

    private Lote map(ResultSet rs, int rowNum) throws SQLException {
        return new Lote(Rows.uuid(rs, "id"), null,
                Rows.uuid(rs, "propiedad_id"), rs.getString("codigo"), rs.getString("nombre"),
                rs.getString("descripcion"), EstadoLote.valueOf(rs.getString("estado")),
                rs.getString("fecha_apertura") == null ? null : LocalDate.parse(rs.getString("fecha_apertura")),
                rs.getString("fecha_cierre") == null ? null : LocalDate.parse(rs.getString("fecha_cierre")),
                rs.getLong("version"), (Integer) rs.getObject("cantidad_maxima"), rs.getLong("cantidad_actual"));
    }

    private MembresiaLote mapMembership(ResultSet rs, int rowNum) throws SQLException {
        Instant salida = Rows.instant(rs, "fecha_salida");
        return new MembresiaLote(Rows.uuid(rs, "id"), Rows.uuid(rs, "lote_id"),
                Rows.uuid(rs, "animal_id"), Rows.instant(rs, "fecha_ingreso"), salida,
                rs.getString("motivo_ingreso"), rs.getString("motivo_salida"), rs.getString("observacion"),
                rs.getString("modo"), Rows.uuid(rs, "ingresado_por"),
                Rows.uuid(rs, "salida_por"), rs.getLong("version"), rs.getString("animal_codigo"), rs.getString("animal_nombre"));
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.LOT_VERSION_CONFLICT : ErrorCode.LOT_NOT_FOUND);
    }
}
