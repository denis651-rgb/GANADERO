package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcIdentificadorRepository implements IdentificadorRepository {
    private final JdbcClient jdbc;

    JdbcIdentificadorRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<IdentificadorAnimal> findByAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql("select * from ganado.identificadores_animal where animal_id=:animal and empresa_id=:e order by principal desc, created_at")
                .param("animal", animalId).param("e", empresa).query(this::map).list();
    }

    @Override
    public Optional<IdentificadorAnimal> findById(UUID id, UUID animalId, UUID empresa) {
        return jdbc.sql("select * from ganado.identificadores_animal where id=:id and animal_id=:animal and empresa_id=:e")
                .param("id", id).param("animal", animalId).param("e", empresa).query(this::map).optional();
    }

    @Override
    public Optional<IdentificadorAnimal> findByQrIdentifier(UUID id, UUID empresa) {
        return jdbc.sql("select * from ganado.identificadores_animal where id=:id and empresa_id=:e and tipo='QR'")
                .param("id", id).param("e", empresa).query(this::map).optional();
    }

    @Override
    public Optional<IdentificadorAnimal> findActiveQr(UUID animalId, UUID empresa) {
        return jdbc.sql("select * from ganado.identificadores_animal where animal_id=:animal and empresa_id=:e and tipo='QR' and estado='ACTIVO' order by created_at desc limit 1")
                .param("animal", animalId).param("e", empresa).query(this::map).optional();
    }

    @Override
    public IdentificadorAnimal create(IdentificadorAnimal i, UUID actor) {
        try {
            Map<String, Object> p = params(i);
            p.put("actor", actor);
            jdbc.sql("""
                    insert into ganado.identificadores_animal(id,empresa_id,animal_id,tipo,valor,principal,estado,
                        fecha_asignacion,asignado_por,observaciones,payload,created_by,updated_by)
                    values(:id,:e,:animal,:tipo,:valor,:principal,:estado,:asignacion,:actor,:obs,:payload,:actor,:actor)""")
                    .params(p).update();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_EXISTS);
        }
        return findById(i.id(), i.animalId(), i.empresaId()).orElseThrow();
    }

    @Override
    public IdentificadorAnimal update(IdentificadorAnimal i, UUID actor) {
        try {
            Map<String, Object> p = params(i);
            p.put("actor", actor);
            int changed = jdbc.sql("""
                    update ganado.identificadores_animal set tipo=:tipo,valor=:valor,principal=:principal,
                        observaciones=:obs,updated_at=now(),updated_by=:actor,version=version+1
                    where id=:id and animal_id=:animal and empresa_id=:e and estado='ACTIVO' and version=:version""")
                    .params(p).update();
            if (changed == 0) throw missingOrConflict(findById(i.id(), i.animalId(), i.empresaId()).isPresent());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_EXISTS);
        }
        IdentificadorAnimal after = findById(i.id(), i.animalId(), i.empresaId()).orElseThrow();
        return after;
    }

    @Override
    public IdentificadorAnimal retire(UUID id, UUID animalId, UUID empresa, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update ganado.identificadores_animal set estado='RETIRADO',principal=false,fecha_retiro=now(),motivo_retiro=:motivo,
                    retirado_por=:actor,updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and animal_id=:animal and empresa_id=:e and estado='ACTIVO' and version=:version""")
                .param("id", id).param("animal", animalId).param("e", empresa)
                .param("motivo", motivo).param("actor", actor).param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(id, animalId, empresa).isPresent());
        return findById(id, animalId, empresa).orElseThrow();
    }

    @Override
    public IdentificadorAnimal setPrincipal(UUID id, UUID animalId, UUID empresa, long version, UUID actor) {
        try {
            int changed = jdbc.sql("""
                    update ganado.identificadores_animal set principal=true,updated_at=now(),updated_by=:actor,version=version+1
                    where id=:id and animal_id=:animal and empresa_id=:e and estado='ACTIVO' and version=:version""")
                    .param("id", id).param("animal", animalId).param("e", empresa)
                    .param("actor", actor).param("version", version).update();
            if (changed == 0) throw missingOrConflict(findById(id, animalId, empresa).isPresent());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_EXISTS);
        }
        return findById(id, animalId, empresa).orElseThrow();
    }

    @Override
    public void clearPrincipal(UUID animalId, UUID empresa, UUID exceptoId) {
        if (exceptoId == null) {
            jdbc.sql("update ganado.identificadores_animal set principal=false,updated_at=now() where animal_id=:animal and empresa_id=:e and estado='ACTIVO' and principal=true")
                    .param("animal", animalId).param("e", empresa).update();
        } else {
            jdbc.sql("update ganado.identificadores_animal set principal=false,updated_at=now() where animal_id=:animal and empresa_id=:e and estado='ACTIVO' and principal=true and id<>:id")
                    .param("animal", animalId).param("e", empresa).param("id", exceptoId).update();
        }
    }

    @Override
    public void lockActiveIdentifiers(UUID animalId, UUID empresa) {
        jdbc.sql("select id from ganado.identificadores_animal where empresa_id=:e and animal_id=:animal and estado='ACTIVO' for update")
                .param("e", empresa).param("animal", animalId).query(UUID.class).list();
    }

    private Map<String, Object> params(IdentificadorAnimal i) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", i.id());
        p.put("e", i.empresaId());
        p.put("animal", i.animalId());
        p.put("tipo", i.tipo().name());
        p.put("valor", i.valor());
        p.put("principal", i.principal());
        p.put("estado", i.estado().name());
        p.put("asignacion", java.sql.Timestamp.from(i.fechaAsignacion()));
        p.put("obs", i.observaciones());
        p.put("payload", i.payload());
        p.put("version", i.version());
        return p;
    }

    private IdentificadorAnimal map(ResultSet rs, int rowNum) throws SQLException {
        Instant retiro = rs.getTimestamp("fecha_retiro") == null ? null : rs.getTimestamp("fecha_retiro").toInstant();
        return new IdentificadorAnimal(
                rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                rs.getObject("animal_id", UUID.class), TipoIdentificador.valueOf(rs.getString("tipo")),
                rs.getString("valor"), rs.getBoolean("principal"),
                EstadoIdentificador.valueOf(rs.getString("estado")),
                rs.getTimestamp("fecha_asignacion").toInstant(), retiro, rs.getString("motivo_retiro"),
                rs.getObject("asignado_por", UUID.class), rs.getObject("retirado_por", UUID.class),
                rs.getString("observaciones"), rs.getString("payload"),
                rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"));
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.IDENTIFIER_VERSION_CONFLICT : ErrorCode.IDENTIFIER_NOT_FOUND);
    }
}
