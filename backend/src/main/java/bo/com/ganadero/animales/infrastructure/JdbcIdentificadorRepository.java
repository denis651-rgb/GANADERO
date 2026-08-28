package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.db.Rows;
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
        return jdbc.sql("select * from identificador_animal where animal_id=:animal order by principal desc, created_at")
                .param("animal", animalId.toString()).query(this::map).list();
    }

    @Override
    public Optional<IdentificadorAnimal> findById(UUID id, UUID animalId, UUID empresa) {
        return jdbc.sql("select * from identificador_animal where id=:id and animal_id=:animal")
                .param("id", id.toString()).param("animal", animalId.toString()).query(this::map).optional();
    }

    @Override
    public Optional<IdentificadorAnimal> findByQrIdentifier(UUID id, UUID empresa) {
        return jdbc.sql("select * from identificador_animal where id=:id and tipo='QR'")
                .param("id", id.toString()).query(this::map).optional();
    }

    @Override
    public Optional<IdentificadorAnimal> findActiveQr(UUID animalId, UUID empresa) {
        return jdbc.sql("select * from identificador_animal where animal_id=:animal and tipo='QR' and estado='ACTIVO' order by created_at desc limit 1")
                .param("animal", animalId.toString()).query(this::map).optional();
    }

    @Override
    public IdentificadorAnimal create(IdentificadorAnimal i, UUID actor) {
        try {
            Map<String, Object> p = params(i);
            p.put("actor", actor.toString());
            jdbc.sql("""
                    insert into identificador_animal(id,animal_id,tipo,valor,principal,estado,
                        fecha_asignacion,asignado_por,observaciones,payload,created_by,updated_by)
                    values(:id,:animal,:tipo,:valor,:principal,:estado,:asignacion,:actor,:obs,:payload,:actor,:actor)""")
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
            p.put("actor", actor.toString());
            int changed = jdbc.sql("""
                    update identificador_animal set tipo=:tipo,valor=:valor,principal=:principal,
                        observaciones=:obs,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                    where id=:id and animal_id=:animal and estado='ACTIVO' and version=:version""")
                    .params(p).update();
            if (changed == 0) throw missingOrConflict(findById(i.id(), i.animalId(), i.empresaId()).isPresent());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_EXISTS);
        }
        return findById(i.id(), i.animalId(), i.empresaId()).orElseThrow();
    }

    @Override
    public IdentificadorAnimal retire(UUID id, UUID animalId, UUID empresa, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update identificador_animal set estado='RETIRADO',principal=0,fecha_retiro=strftime('%Y-%m-%dT%H:%M:%fZ','now'),motivo_retiro=:motivo,
                    retirado_por=:actor,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                where id=:id and animal_id=:animal and estado='ACTIVO' and version=:version""")
                .param("id", id.toString()).param("animal", animalId.toString())
                .param("motivo", motivo).param("actor", actor.toString()).param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(id, animalId, empresa).isPresent());
        return findById(id, animalId, empresa).orElseThrow();
    }

    @Override
    public IdentificadorAnimal setPrincipal(UUID id, UUID animalId, UUID empresa, long version, UUID actor) {
        try {
            int changed = jdbc.sql("""
                    update identificador_animal set principal=1,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),updated_by=:actor,version=version+1
                    where id=:id and animal_id=:animal and estado='ACTIVO' and version=:version""")
                    .param("id", id.toString()).param("animal", animalId.toString())
                    .param("actor", actor.toString()).param("version", version).update();
            if (changed == 0) throw missingOrConflict(findById(id, animalId, empresa).isPresent());
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_EXISTS);
        }
        return findById(id, animalId, empresa).orElseThrow();
    }

    @Override
    public void clearPrincipal(UUID animalId, UUID empresa, UUID exceptoId) {
        if (exceptoId == null) {
            jdbc.sql("update identificador_animal set principal=0,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now') where animal_id=:animal and estado='ACTIVO' and principal=1")
                    .param("animal", animalId.toString()).update();
        } else {
            jdbc.sql("update identificador_animal set principal=0,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now') where animal_id=:animal and estado='ACTIVO' and principal=1 and id<>:id")
                    .param("animal", animalId.toString()).param("id", exceptoId.toString()).update();
        }
    }

    @Override
    public void lockActiveIdentifiers(UUID animalId, UUID empresa) {
        jdbc.sql("select id from identificador_animal where animal_id=:animal and estado='ACTIVO'")
                .param("animal", animalId.toString()).query(String.class).list();
    }

    private Map<String, Object> params(IdentificadorAnimal i) {
        Map<String, Object> p = new HashMap<>();
        p.put("id", i.id().toString());
        p.put("animal", i.animalId().toString());
        p.put("tipo", i.tipo().name());
        p.put("valor", i.valor());
        p.put("principal", i.principal());
        p.put("estado", i.estado().name());
        p.put("asignacion", i.fechaAsignacion().toString());
        p.put("obs", i.observaciones());
        p.put("payload", i.payload());
        p.put("version", i.version());
        return p;
    }

    private IdentificadorAnimal map(ResultSet rs, int rowNum) throws SQLException {
        Instant retiro = Rows.instant(rs, "fecha_retiro");
        return new IdentificadorAnimal(
                Rows.uuid(rs, "id"), null,
                Rows.uuid(rs, "animal_id"), TipoIdentificador.valueOf(rs.getString("tipo")),
                rs.getString("valor"), rs.getBoolean("principal"),
                EstadoIdentificador.valueOf(rs.getString("estado")),
                Rows.instant(rs, "fecha_asignacion"), retiro, rs.getString("motivo_retiro"),
                Rows.uuid(rs, "asignado_por"), Rows.uuid(rs, "retirado_por"),
                rs.getString("observaciones"), rs.getString("payload"),
                Rows.instant(rs, "created_at"), Rows.instant(rs, "updated_at"),
                rs.getLong("version"));
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.IDENTIFIER_VERSION_CONFLICT : ErrorCode.IDENTIFIER_NOT_FOUND);
    }
}
