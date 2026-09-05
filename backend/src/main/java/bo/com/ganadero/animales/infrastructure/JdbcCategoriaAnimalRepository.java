package bo.com.ganadero.animales.infrastructure; import bo.com.ganadero.animales.domain.*; import bo.com.ganadero.shared.db.Rows; import org.springframework.jdbc.core.simple.JdbcClient; import org.springframework.stereotype.Repository; import java.util.*;
@Repository class JdbcCategoriaAnimalRepository implements CategoriaAnimalRepository {
    private final JdbcClient jdbc;
    JdbcCategoriaAnimalRepository(JdbcClient j){jdbc=j;}
    public List<CategoriaAnimal> findActive(UUID e){return jdbc.sql("select * from categoria_animal where activo order by orden_evaluacion,edad_min_meses,nombre").query(this::map).list();}
    public Optional<CategoriaAnimal> findById(UUID id,UUID e){return jdbc.sql("select * from categoria_animal where id=:id and activo").param("id",id.toString()).query(this::map).optional();}
    public List<CategoriaAnimal> findAllIncludingInactive(){return jdbc.sql("select * from categoria_animal order by sexo_aplicable,orden_evaluacion,edad_min_meses,nombre").query(this::map).list();}
    public CategoriaAnimal crear(CategoriaAnimal c){
        jdbc.sql("insert into categoria_animal(id,codigo,nombre,sexo_aplicable,edad_min_meses,edad_max_meses,descripcion,activo,clasificacion_automatica,orden_evaluacion) values(:id,:codigo,:nombre,:sexo,:min,:max,:descripcion,:activo,:automatica,:orden)")
            .param("id",c.id().toString()).param("codigo",c.codigo()).param("nombre",c.nombre()).param("sexo",c.sexoAplicable())
            .param("min",c.edadMinMeses()).param("max",c.edadMaxMeses()).param("descripcion",c.descripcion())
            .param("activo",c.activo()?1:0).param("automatica",c.clasificacionAutomatica()?1:0).param("orden",c.ordenEvaluacion()).update();
        return findByIdIncludingInactive(c.id());
    }
    public CategoriaAnimal actualizar(CategoriaAnimal c){
        jdbc.sql("update categoria_animal set nombre=:nombre,sexo_aplicable=:sexo,edad_min_meses=:min,edad_max_meses=:max,descripcion=:descripcion,clasificacion_automatica=:automatica,orden_evaluacion=:orden where id=:id")
            .param("nombre",c.nombre()).param("sexo",c.sexoAplicable()).param("min",c.edadMinMeses()).param("max",c.edadMaxMeses())
            .param("descripcion",c.descripcion()).param("automatica",c.clasificacionAutomatica()?1:0).param("orden",c.ordenEvaluacion())
            .param("id",c.id().toString()).update();
        return findByIdIncludingInactive(c.id());
    }
    public void cambiarEstado(UUID id,boolean activo){jdbc.sql("update categoria_animal set activo=:activo where id=:id").param("activo",activo?1:0).param("id",id.toString()).update();}
    public void eliminar(UUID id){jdbc.sql("delete from categoria_animal where id=:id").param("id",id.toString()).update();}
    private CategoriaAnimal findByIdIncludingInactive(UUID id){return jdbc.sql("select * from categoria_animal where id=:id").param("id",id.toString()).query(this::map).single();}
    private CategoriaAnimal map(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new CategoriaAnimal(Rows.uuid(r,"id"),null,r.getString("codigo"),r.getString("nombre"),r.getString("sexo_aplicable"),(Integer)r.getObject("edad_min_meses"),(Integer)r.getObject("edad_max_meses"),r.getString("descripcion"),r.getBoolean("activo"),r.getBoolean("clasificacion_automatica"),r.getInt("orden_evaluacion"));}
}
