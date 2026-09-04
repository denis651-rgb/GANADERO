package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.domain.Parentesco;
import bo.com.ganadero.animales.domain.ParentescoRepository;
import bo.com.ganadero.animales.domain.TipoParentesco;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class JdbcParentescoRepository implements ParentescoRepository {
    private final JdbcClient jdbc;

    JdbcParentescoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Parentesco> findByAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql("select * from parentesco where animal_id=:animal order by tipo_parentesco")
                .param("animal", animalId.toString()).query(this::map).list();
    }

    @Override
    public Optional<Parentesco> findById(UUID id, UUID animalId, UUID empresa) {
        return jdbc.sql("select * from parentesco where id=:id and animal_id=:animal")
                .param("id", id.toString()).param("animal", animalId.toString()).query(this::map).optional();
    }

    @Override
    public Optional<UUID> findRegisteredParentId(UUID animalId, UUID empresa) {
        return jdbc.sql("select animal_padre_id from parentesco where animal_id=:animal and animal_padre_id is not null limit 1")
                .param("animal", animalId.toString()).query(String.class).optional().map(UUID::fromString);
    }

    @Override
    public Parentesco create(Parentesco p, UUID actor) {
        try {
            jdbc.sql("""
                    insert into parentesco(id,animal_id,tipo_parentesco,animal_padre_id,
                        nombre_externo,raza_externa_id,registro_genealogico,fecha_registro,registrado_por)
                    values(:id,:animal,:tipo,:padre,:externo,:raza,:registro,strftime('%Y-%m-%dT%H:%M:%fZ','now'),:actor)""")
                    .param("id", p.id().toString()).param("animal", p.animalId().toString())
                    .param("tipo", p.tipo().name()).param("padre", p.animalPadreId() == null ? null : p.animalPadreId().toString())
                    .param("externo", p.nombreExterno()).param("raza", p.razaExternaId() == null ? null : p.razaExternaId().toString())
                    .param("registro", p.registroGenealogico()).param("actor", actor.toString())
                    .update();
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.PARENTESCO_ALREADY_EXISTS);
        }
        return findById(p.id(), p.animalId(), p.empresaId()).orElseThrow();
    }

    @Override
    public Parentesco update(Parentesco p, UUID actor) {
        try {
            int changed = jdbc.sql("""
                    update parentesco set tipo_parentesco=:tipo,animal_padre_id=:padre,nombre_externo=:externo,
                        raza_externa_id=:raza,registro_genealogico=:registro,registrado_por=:actor
                    where id=:id and animal_id=:animal""")
                    .param("tipo", p.tipo().name()).param("padre", p.animalPadreId() == null ? null : p.animalPadreId().toString())
                    .param("externo", p.nombreExterno()).param("raza", p.razaExternaId() == null ? null : p.razaExternaId().toString())
                    .param("registro", p.registroGenealogico()).param("actor", actor.toString())
                    .param("id", p.id().toString()).param("animal", p.animalId().toString())
                    .update();
            if (changed == 0) throw new BusinessException(ErrorCode.PARENTESCO_NOT_FOUND);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.PARENTESCO_CYCLE);
        }
        return findById(p.id(), p.animalId(), p.empresaId()).orElseThrow();
    }

    @Override
    public void delete(UUID id, UUID animalId, UUID empresa) {
        jdbc.sql("delete from parentesco where id=:id and animal_id=:animal")
                .param("id", id.toString()).param("animal", animalId.toString()).update();
    }

    private Parentesco map(ResultSet rs, int rowNum) throws SQLException {
        return new Parentesco(
                Rows.uuid(rs, "id"), null,
                Rows.uuid(rs, "animal_id"), TipoParentesco.valueOf(rs.getString("tipo_parentesco")),
                Rows.uuid(rs, "animal_padre_id"), rs.getString("nombre_externo"),
                Rows.uuid(rs, "raza_externa_id"), rs.getString("registro_genealogico"),
                Rows.instant(rs, "fecha_registro"), Rows.uuid(rs, "registrado_por"));
    }
}
