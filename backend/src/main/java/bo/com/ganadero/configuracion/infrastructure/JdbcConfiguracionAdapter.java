package bo.com.ganadero.configuracion.infrastructure;
import bo.com.ganadero.configuracion.domain.*; import bo.com.ganadero.shared.db.Rows; import bo.com.ganadero.shared.error.*;
import org.springframework.jdbc.core.simple.JdbcClient; import org.springframework.stereotype.Repository;
import java.sql.ResultSet; import java.sql.SQLException; import java.util.*;
@Repository class JdbcConfiguracionAdapter implements ConfiguracionRepository {
 private final JdbcClient jdbc; JdbcConfiguracionAdapter(JdbcClient j){jdbc=j;}
 private static final String SELECT="select id,zona_horaria,moneda,unidad_peso,unidad_superficie,dias_alerta_preparto,dias_alerta_vacunacion,dias_sin_pesaje,dias_alerta_destete,dias_diagnostico_post_servicio,dias_gestacion_estimada,comprimir_imagenes,calidad_imagen,nombre_usuario,(pin_hash is not null) as pin_configurado,version from configuracion";
 public Configuracion get(){return jdbc.sql(SELECT).query(this::map).single();}
 public Configuracion update(Configuracion p,UUID actor){
  int changed=jdbc.sql("""
    update configuracion set zona_horaria=coalesce(:tz,zona_horaria),moneda=coalesce(:moneda,moneda),
     unidad_peso=coalesce(:up,unidad_peso),unidad_superficie=coalesce(:us,unidad_superficie),
     dias_alerta_preparto=coalesce(:dap,dias_alerta_preparto),dias_alerta_vacunacion=coalesce(:dav,dias_alerta_vacunacion),
     dias_sin_pesaje=coalesce(:dsp,dias_sin_pesaje),dias_alerta_destete=coalesce(:dad,dias_alerta_destete),
     dias_diagnostico_post_servicio=coalesce(:ddps,dias_diagnostico_post_servicio),dias_gestacion_estimada=coalesce(:dge,dias_gestacion_estimada),
     comprimir_imagenes=coalesce(:ci,comprimir_imagenes),calidad_imagen=coalesce(:cal,calidad_imagen),nombre_usuario=coalesce(:nu,nombre_usuario),
     updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),version=version+1
    where version=:v
    """).params(params(p)).update();
  if(changed==0)throw new BusinessException(ErrorCode.VERSION_CONFLICT);
  return get();
 }
 public void updatePin(String pinHash){jdbc.sql("update configuracion set pin_hash=:hash,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now')").param("hash",pinHash).update();}
 private Map<String,Object> params(Configuracion p){
  Map<String,Object> m=new HashMap<>();
  m.put("tz",p.zonaHoraria());m.put("moneda",p.moneda());m.put("up",p.unidadPeso());m.put("us",p.unidadSuperficie());
  m.put("dap",p.diasAlertaPreparto());m.put("dav",p.diasAlertaVacunacion());m.put("dsp",p.diasSinPesaje());m.put("dad",p.diasAlertaDestete());
  m.put("ddps",p.diasDiagnosticoPostServicio());m.put("dge",p.diasGestacionEstimada());
  m.put("ci",p.comprimirImagenes());m.put("cal",p.calidadImagen());m.put("nu",p.nombreUsuario());m.put("v",p.version());
  return m;
 }
 private Configuracion map(ResultSet r,int n)throws SQLException{
  return new Configuracion(Rows.uuid(r,"id"),r.getString("zona_horaria"),r.getString("moneda"),r.getString("unidad_peso"),r.getString("unidad_superficie"),
   r.getInt("dias_alerta_preparto"),r.getInt("dias_alerta_vacunacion"),r.getInt("dias_sin_pesaje"),r.getInt("dias_alerta_destete"),
   r.getInt("dias_diagnostico_post_servicio"),r.getInt("dias_gestacion_estimada"),
   r.getBoolean("comprimir_imagenes"),r.getInt("calidad_imagen"),r.getString("nombre_usuario"),r.getBoolean("pin_configurado"),r.getLong("version"));
 }
}
