package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;

    JdbcIdentificadorRepository(JdbcClient jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
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
    public IdentificadorAnimal create(IdentificadorAnimal i, UUID actor) {
        try {
            Map<String, Object> p = params(i);
            p.put("actor", actor);
            jdbc.sql("""
                    insert into ganado.identificadores_animal(id,empresa_id,animal_id,tipo,valor,principal,estado,
                        fecha_asignacion,asignado_por,observaciones,created_by,updated_by)
                    values(:id,:e,:animal,:tipo,:valor,:principal,:estado,:asignacion,:actor,:obs,:actor,:actor)""")
                    .params(p).update();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_EXISTS);
        }
        insertEvent(i, actor, "IDENTIFICADOR " + i.tipo().name() + " asignado", "asignado", i.valor());
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
        insertEvent(i, actor, "Identificador " + i.tipo().name() + " actualizado", "actualizado", i.valor());
        return findById(i.id(), i.animalId(), i.empresaId()).orElseThrow();
    }

    @Override
    public IdentificadorAnimal retire(UUID id, UUID animalId, UUID empresa, String motivo, long version, UUID actor) {
        int changed = jdbc.sql("""
                update ganado.identificadores_animal set estado='RETIRADO',fecha_retiro=now(),motivo_retiro=:motivo,
                    retirado_por=:actor,updated_at=now(),updated_by=:actor,version=version+1
                where id=:id and animal_id=:animal and empresa_id=:e and estado='ACTIVO' and version=:version""")
                .param("id", id).param("animal", animalId).param("e", empresa)
                .param("motivo", motivo).param("actor", actor).param("version", version).update();
        if (changed == 0) throw missingOrConflict(findById(id, animalId, empresa).isPresent());
        IdentificadorAnimal saved = findById(id, animalId, empresa).orElseThrow();
        insertEvent(saved, actor, "Identificador " + saved.tipo().name() + " retirado", "retirado", motivo);
        return saved;
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

    private void insertEvent(IdentificadorAnimal i, UUID actor, String titulo, String accion, String detalle) {
        String metadata = "{}";
        try {
            metadata = objectMapper.writeValueAsString(Map.of("tipo", i.tipo().name(), "valor", i.valor(), "accion", accion));
        } catch (Exception ignored) {
            // metadata opcional
        }
        jdbc.sql("""
                insert into ganado.eventos_animal(id,empresa_id,animal_id,tipo,titulo,descripcion,modulo_origen,
                    registro_origen,dispositivo,metadata,registrado_por,created_by,fecha_evento)
                values(:id,:e,:animal,'IDENTIFICADOR',:titulo,:detalle,'IDENTIFICADORES',:registro,null,:metadata::jsonb,:actor,:actor,now())""")
                .param("id", UUID.randomUUID())
                .param("e", i.empresaId())
                .param("animal", i.animalId())
                .param("titulo", titulo)
                .param("detalle", detalle)
                .param("registro", i.id())
                .param("metadata", metadata)
                .param("actor", actor)
                .update();
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
                rs.getString("observaciones"), rs.getLong("version"));
    }

    private BusinessException missingOrConflict(boolean exists) {
        return new BusinessException(exists ? ErrorCode.VERSION_CONFLICT : ErrorCode.IDENTIFIER_NOT_FOUND);
    }
}
