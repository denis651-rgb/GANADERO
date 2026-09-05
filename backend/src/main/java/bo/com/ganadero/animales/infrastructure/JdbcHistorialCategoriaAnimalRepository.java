package bo.com.ganadero.animales.infrastructure;
import bo.com.ganadero.animales.domain.HistorialCategoriaAnimal;
import bo.com.ganadero.animales.domain.HistorialCategoriaAnimalRepository;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

@Repository class JdbcHistorialCategoriaAnimalRepository implements HistorialCategoriaAnimalRepository {
    private final JdbcClient jdbc;
    JdbcHistorialCategoriaAnimalRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    public void crear(HistorialCategoriaAnimal h) {
        jdbc.sql("insert into historial_categoria_animal(id,animal_id,categoria_anterior_id,categoria_nueva_id,fecha_cambio,tipo_cambio,motivo,usuario_id,edad_dias,edad_confirmada,categoria_config_id) values(:id,:animal,:anterior,:nueva,:fecha,:tipo,:motivo,:usuario,:edadDias,:confirmada,:config)")
                .param("id", h.id().toString())
                .param("animal", h.animalId().toString())
                .param("anterior", h.categoriaAnteriorId() == null ? null : h.categoriaAnteriorId().toString())
                .param("nueva", h.categoriaNuevaId().toString())
                .param("fecha", h.fechaCambio().toString())
                .param("tipo", h.tipoCambio())
                .param("motivo", h.motivo())
                .param("usuario", h.usuarioId() == null ? null : h.usuarioId().toString())
                .param("edadDias", h.edadDias())
                .param("confirmada", h.edadConfirmada() ? 1 : 0)
                .param("config", h.categoriaConfigId() == null ? null : h.categoriaConfigId().toString())
                .update();
    }

    public List<HistorialCategoriaAnimal> listar(UUID animalId) {
        return jdbc.sql("select * from historial_categoria_animal where animal_id=:animal order by fecha_cambio desc")
                .param("animal", animalId.toString()).query(this::map).list();
    }

    public boolean existeParaCategoria(UUID categoriaId) {
        return jdbc.sql("select exists(select 1 from historial_categoria_animal where categoria_anterior_id=:c or categoria_nueva_id=:c)")
                .param("c", categoriaId.toString()).query(Boolean.class).single();
    }

    private HistorialCategoriaAnimal map(ResultSet r, int n) throws SQLException {
        return new HistorialCategoriaAnimal(Rows.uuid(r, "id"), Rows.uuid(r, "animal_id"),
                Rows.uuid(r, "categoria_anterior_id"), Rows.uuid(r, "categoria_nueva_id"),
                Rows.instant(r, "fecha_cambio"), r.getString("tipo_cambio"), r.getString("motivo"),
                Rows.uuid(r, "usuario_id"), Rows.longOrNull(r, "edad_dias"), r.getBoolean("edad_confirmada"),
                Rows.uuid(r, "categoria_config_id"));
    }
}
