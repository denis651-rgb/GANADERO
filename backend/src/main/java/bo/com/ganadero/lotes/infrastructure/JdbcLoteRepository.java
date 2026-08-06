package bo.com.ganadero.lotes.infrastructure;

import bo.com.ganadero.lotes.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
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
class JdbcLoteRepository implements LoteRepository {
    private final JdbcClient jdbc;

    JdbcLoteRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LotePage findAll(UUID empresa, EstadoLote estado, String search, int page, int size) {
        StringBuilder where = new StringBuilder(" where l.empresa_id=:e");
        Map<String, Object> params = new HashMap<>();
        params.put("e", empresa);
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
    public Lote close(UUID id, UUID empresa, long version, UUID actor) {
        int changed = jdbc.sql("""
                update ganado.lotes_ganaderos set estado='CERRADO',fecha_cierre=current_date,
                    updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and empresa_id=:e and version=:version and estado='ABIERTO'""")
                .param("actor", actor).param("id", id).param("e", empresa).param("version", version).update();
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
    public List<UUID> findActiveAnimalsInLote(List<UUID> animalIds, UUID empresa) {
        if (animalIds.isEmpty()) return List.of();
        return jdbc.sql("select distinct animal_id from ganado.membresias_lote where empresa_id=:e and fecha_salida is null and animal_id in (:ids)")
                .param("e", empresa).param("ids", animalIds).query(UUID.class).list();
    }

    @Override
    public Optional<Lote> findActiveLotOfAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql("""
                select l.* from ganado.lotes_ganaderos l
                join ganado.membresias_lote m on m.lote_id=l.id
                where m.animal_id=:animal and m.empresa_id=:e and m.fecha_salida is null and l.estado='ABIERTO' limit 1""")
                .param("animal", animalId).param("e", empresa).query(this::map).optional();
    }

    @Override
    public boolean hasActiveAnimals(UUID loteId, UUID empresa) {
        return jdbc.sql("select exists(select 1 from ganado.membresias_lote where lote_id=:lote and empresa_id=:e and fecha_salida is null)")
                .param("lote", loteId).param("e", empresa).query(Boolean.class).single();
    }

    @Override
    public void openMembership(UUID loteId, UUID animalId, UUID empresa, UUID actor) {
        jdbc.sql("""
                insert into ganado.membresias_lote(id,empresa_id,lote_id,animal_id,fecha_ingreso,ingresado_por)
                values(:id,:e,:lote,:animal,now(),:actor)""")
                .param("id", UUID.randomUUID()).param("e", empresa).param("lote", loteId)
                .param("animal", animalId).param("actor", actor).update();
        insertEvent(loteId, animalId, empresa, actor, "Ingresado al lote", "ingreso", "LOTE");
    }

    @Override
    public void closeMembership(UUID loteId, UUID animalId, UUID empresa, String motivo, UUID actor) {
        jdbc.sql("""
                update ganado.membresias_lote set fecha_salida=now(),motivo_salida=:motivo,salida_por=:actor
                where lote_id=:lote and animal_id=:animal and empresa_id=:e and fecha_salida is null""")
                .param("motivo", motivo).param("actor", actor).param("lote", loteId)
                .param("animal", animalId).param("e", empresa).update();
        insertEvent(loteId, animalId, empresa, actor, "Retirado del lote", "salida", "LOTE");
    }

    private void insertEvent(UUID loteId, UUID animalId, UUID empresa, UUID actor, String titulo, String detalle, String modulo) {
        jdbc.sql("""
                insert into ganado.eventos_animal(id,empresa_id,animal_id,tipo,titulo,descripcion,modulo_origen,
                    registro_origen,dispositivo,metadata,registrado_por,created_by,fecha_evento)
                values(:id,:e,:animal,'LOTE',:titulo,:detalle,:modulo,:registro,null,'{}'::jsonb,:actor,:actor,now())""")
                .param("id", UUID.randomUUID()).param("e", empresa).param("animal", animalId)
                .param("titulo", titulo).param("detalle", detalle).param("modulo", modulo)
                .param("registro", loteId).param("actor", actor).update();
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
                rs.getString("motivo_salida"), rs.getObject("ingresado_por", UUID.class),
                rs.getObject("salida_por", UUID.class));
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.VERSION_CONFLICT : ErrorCode.LOT_NOT_FOUND);
    }
}
