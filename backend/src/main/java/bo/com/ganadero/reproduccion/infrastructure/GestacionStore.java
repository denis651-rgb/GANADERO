package bo.com.ganadero.reproduccion.infrastructure;

import bo.com.ganadero.reproduccion.domain.GestacionCiclo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.*;
import java.util.*;

@Repository
public class GestacionStore {
 private final JdbcClient jdbc;
 public GestacionStore(JdbcClient jdbc){this.jdbc=jdbc;}
 private static UUID uuid(String s){return s==null?null:UUID.fromString(s);}
 private static LocalDate date(String s){return s==null?null:LocalDate.parse(s);}
 private GestacionCiclo map(ResultSet r,int row)throws SQLException{return new GestacionCiclo(uuid(r.getString("id")),uuid(r.getString("animal_id")),
  uuid(r.getString("servicio_id")),uuid(r.getString("diagnostico_id")),r.getBoolean("antecedentes_desconocidos"),date(r.getString("fecha_confirmacion")),
  date(r.getString("fecha_inicio_estimada")),r.getString("observaciones"),r.getString("estado"),date(r.getString("fecha_cierre")),uuid(r.getString("evento_id")));}
 public List<GestacionCiclo> list(UUID empresa,UUID animal){return jdbc.sql("select * from gestacion_ciclo where empresa_id=? and animal_id=? order by fecha_confirmacion desc,creado_en desc")
  .params(empresa.toString(),animal.toString()).query(this::map).list();}
 public Optional<GestacionCiclo> find(UUID empresa,UUID id){return jdbc.sql("select * from gestacion_ciclo where empresa_id=? and id=?").params(empresa.toString(),id.toString()).query(this::map).optional();}
 public void create(UUID empresa,UUID actor,GestacionCiclo g){jdbc.sql("""
  insert into gestacion_ciclo(id,empresa_id,animal_id,servicio_id,diagnostico_id,antecedentes_desconocidos,fecha_confirmacion,fecha_inicio_estimada,observaciones,estado,creado_por,creado_en)
  values(:id,:empresa,:animal,:servicio,:diagnostico,:desconocidos,:fecha,:inicio,:obs,'ABIERTA',:actor,:ahora)
  """).param("id",g.id().toString()).param("empresa",empresa.toString()).param("animal",g.animalId().toString())
  .param("servicio",g.servicioId()==null?null:g.servicioId().toString()).param("diagnostico",g.diagnosticoId()==null?null:g.diagnosticoId().toString())
  .param("desconocidos",g.antecedentesDesconocidos()).param("fecha",g.fechaConfirmacion().toString()).param("inicio",g.fechaInicioEstimada()==null?null:g.fechaInicioEstimada().toString())
  .param("obs",g.observaciones()).param("actor",actor.toString()).param("ahora",Instant.now().toString()).update();}
 public boolean close(UUID empresa,UUID actor,UUID id,String estado,LocalDate fecha,UUID evento){return jdbc.sql("update gestacion_ciclo set estado=?,fecha_cierre=?,evento_id=?,cerrado_por=? where empresa_id=? and id=? and estado='ABIERTA'")
  .params(estado,fecha.toString(),evento.toString(),actor.toString(),empresa.toString(),id.toString()).update()==1;}
 public void link(boolean parto,UUID evento,UUID ciclo){jdbc.sql("update "+(parto?"parto":"aborto")+" set ciclo_gestacion_id=? where id=?").params(ciclo.toString(),evento.toString()).update();}
 public void linkDiagnostico(UUID diagnostico,UUID ciclo){jdbc.sql("insert into gestacion_diagnostico(diagnostico_id,ciclo_id) values(?,?) on conflict(diagnostico_id) do nothing").params(diagnostico.toString(),ciclo.toString()).update();}
 public Set<UUID> diagnosticos(UUID ciclo){return new HashSet<>(jdbc.sql("select diagnostico_id from gestacion_diagnostico where ciclo_id=?").param(ciclo.toString()).query(String.class).list().stream().map(UUID::fromString).toList());}
 public Optional<LocalDate> lastEnd(UUID empresa,UUID animal){return jdbc.sql("""
  select max(fecha) from (
   select fecha_cierre fecha from gestacion_ciclo where empresa_id=:empresa and animal_id=:animal
   union all select fecha_parto fecha from parto where madre_id=:animal and estado='ACTIVO'
   union all select fecha_evento fecha from aborto where animal_id=:animal and estado='ACTIVO'
  )
  """).param("empresa",empresa.toString()).param("animal",animal.toString()).query(String.class).optional().map(LocalDate::parse);}
}
