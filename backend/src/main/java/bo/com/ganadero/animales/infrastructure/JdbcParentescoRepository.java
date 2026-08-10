package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.domain.Parentesco;
import bo.com.ganadero.animales.domain.ParentescoRepository;
import bo.com.ganadero.animales.domain.TipoParentesco;
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
        return jdbc.sql("select * from ganado.parentescos where animal_id=:animal and empresa_id=:e order by tipo_parentesco")
                .param("animal", animalId).param("e", empresa).query(this::map).list();
    }

    @Override
    public Optional<Parentesco> findById(UUID id, UUID animalId, UUID empresa) {
        return jdbc.sql("select * from ganado.parentescos where id=:id and animal_id=:animal and empresa_id=:e")
                .param("id", id).param("animal", animalId).param("e", empresa).query(this::map).optional();
    }

    @Override
    public Optional<UUID> findRegisteredParentId(UUID animalId, UUID empresa) {
        return jdbc.sql("select animal_padre_id from ganado.parentescos where animal_id=:animal and empresa_id=:e and animal_padre_id is not null limit 1")
                .param("animal", animalId).param("e", empresa).query(UUID.class).optional();
    }

    @Override
    public Parentesco create(Parentesco p, UUID actor) {
        try {
            jdbc.sql("""
                    insert into ganado.parentescos(id,empresa_id,animal_id,tipo_parentesco,animal_padre_id,
                        nombre_externo,raza_externa_id,registro_genealogico,fecha_registro,registrado_por)
                    values(:id,:e,:animal,:tipo,:padre,:externo,:raza,:registro,now(),:actor)""")
                    .param("id", p.id()).param("e", p.empresaId()).param("animal", p.animalId())
                    .param("tipo", p.tipo().name()).param("padre", p.animalPadreId())
                    .param("externo", p.nombreExterno()).param("raza", p.razaExternaId())
                    .param("registro", p.registroGenealogico()).param("actor", actor)
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
                    update ganado.parentescos set tipo_parentesco=:tipo,animal_padre_id=:padre,nombre_externo=:externo,
                        raza_externa_id=:raza,registro_genealogico=:registro,registrado_por=:actor
                    where id=:id and animal_id=:animal and empresa_id=:e""")
                    .param("tipo", p.tipo().name()).param("padre", p.animalPadreId())
                    .param("externo", p.nombreExterno()).param("raza", p.razaExternaId())
                    .param("registro", p.registroGenealogico()).param("actor", actor)
                    .param("id", p.id()).param("animal", p.animalId()).param("e", p.empresaId())
                    .update();
            if (changed == 0) throw new BusinessException(ErrorCode.PARENTESCO_NOT_FOUND);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.PARENTESCO_CYCLE);
        }
        return findById(p.id(), p.animalId(), p.empresaId()).orElseThrow();
    }

    @Override
    public void delete(UUID id, UUID animalId, UUID empresa) {
        jdbc.sql("delete from ganado.parentescos where id=:id and animal_id=:animal and empresa_id=:e")
                .param("id", id).param("animal", animalId).param("e", empresa).update();
    }

    private Parentesco map(ResultSet rs, int rowNum) throws SQLException {
        return new Parentesco(
                rs.getObject("id", UUID.class), rs.getObject("empresa_id", UUID.class),
                rs.getObject("animal_id", UUID.class), TipoParentesco.valueOf(rs.getString("tipo_parentesco")),
                rs.getObject("animal_padre_id", UUID.class), rs.getString("nombre_externo"),
                rs.getObject("raza_externa_id", UUID.class), rs.getString("registro_genealogico"),
                rs.getTimestamp("fecha_registro").toInstant(), rs.getObject("registrado_por", UUID.class));
    }
}
