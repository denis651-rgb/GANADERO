package bo.com.ganadero.animales.infrastructure; import bo.com.ganadero.animales.domain.*; import bo.com.ganadero.shared.db.Rows; import org.springframework.jdbc.core.simple.JdbcClient; import org.springframework.stereotype.Repository; import java.util.*;
@Repository class JdbcRazaRepository implements RazaRepository {
    private final JdbcClient jdbc;
    JdbcRazaRepository(JdbcClient j){jdbc=j;}
    public List<Raza> findActive(UUID e){return jdbc.sql("select * from raza where activo order by nombre").query(this::map).list();}
    public Optional<Raza> findById(UUID id,UUID e){return jdbc.sql("select * from raza where id=:id and activo").param("id",id.toString()).query(this::map).optional();}
    public Optional<Raza> findByNombre(String nombre){return jdbc.sql("select * from raza where lower(nombre)=lower(:nombre)").param("nombre",nombre).query(this::map).optional();}
    public boolean existeCodigo(String codigo){return jdbc.sql("select count(*) from raza where codigo=:codigo").param("codigo",codigo).query(Integer.class).single()>0;}
    public Raza crear(Raza r){
        jdbc.sql("insert into raza(id,codigo,nombre,especie,descripcion,activo) values(:id,:codigo,:nombre,:especie,:descripcion,1)")
            .param("id",r.id().toString()).param("codigo",r.codigo()).param("nombre",r.nombre())
            .param("especie",r.especie()).param("descripcion",r.descripcion()).update();
        return r;
    }
    private Raza map(java.sql.ResultSet r,int n)throws java.sql.SQLException{return new Raza(Rows.uuid(r,"id"),null,r.getString("codigo"),r.getString("nombre"),r.getString("especie"),r.getString("descripcion"),r.getBoolean("activo"));}
}
