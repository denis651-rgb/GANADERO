package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.domain.EstadoRecordatorio;
import bo.com.ganadero.alertas.domain.Recordatorio;
import bo.com.ganadero.alertas.domain.RecordatorioRepository;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRecordatorioRepository implements RecordatorioRepository {
    private final JdbcClient jdbc;
    public JdbcRecordatorioRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override public Recordatorio guardar(Recordatorio r) {
        jdbc.sql("""
                insert into alertas.recordatorios(id,empresa_id,creado_por,titulo,mensaje,severidad,animal_id,
                  fecha_evento,proxima_ejecucion,cantidad_notificaciones,intervalo_minutos,estado)
                values(:id,:e,:u,:t,:m,:s,:a,:fe,:pe,:c,:i,'ACTIVO')
                """).param("id",r.id()).param("e",r.empresaId()).param("u",r.creadoPor())
                .param("t",r.titulo()).param("m",r.mensaje()).param("s",r.severidad().name())
                .param("a",r.animalId())
                .param("fe",OffsetDateTime.ofInstant(r.fechaEvento(), java.time.ZoneOffset.UTC))
                .param("pe",OffsetDateTime.ofInstant(r.proximaEjecucion(), java.time.ZoneOffset.UTC))
                .param("c",r.cantidadNotificaciones()).param("i",r.intervaloMinutos()).update();
        return buscar(r.id(), r.empresaId()).orElseThrow();
    }
    @Override public List<Recordatorio> listar(UUID empresaId) {
        return jdbc.sql("select * from alertas.recordatorios where empresa_id=:e order by created_at desc")
                .param("e",empresaId).query(this::map).list();
    }
    @Override public Optional<Recordatorio> buscar(UUID id, UUID empresaId) {
        return jdbc.sql("select * from alertas.recordatorios where id=:id and empresa_id=:e")
                .param("id",id).param("e",empresaId).query(this::map).optional();
    }
    @Override public List<Recordatorio> bloquearVencidos(Instant ahora, int limite) {
        return jdbc.sql("select * from alertas.recordatorios where estado='ACTIVO' and proxima_ejecucion<=:n order by proxima_ejecucion for update skip locked limit :l")
                .param("n",OffsetDateTime.ofInstant(ahora, java.time.ZoneOffset.UTC)).param("l",limite).query(this::map).list();
    }
    @Override public void registrarEjecucion(Recordatorio r, Instant siguiente, boolean completado) {
        jdbc.sql("update alertas.recordatorios set notificaciones_generadas=notificaciones_generadas+1,proxima_ejecucion=coalesce(:p,proxima_ejecucion),estado=case when :c then 'COMPLETADO' else estado end,updated_at=now(),version=version+1 where id=:id and empresa_id=:e")
                .param("p",siguiente != null ? OffsetDateTime.ofInstant(siguiente, java.time.ZoneOffset.UTC) : null)
                .param("c",completado).param("id",r.id()).param("e",r.empresaId()).update();
    }
    @Override public Recordatorio cambiarEstado(UUID id, UUID empresaId, EstadoRecordatorio estado, long version) {
        int n=jdbc.sql("update alertas.recordatorios set estado=:s,updated_at=now(),version=version+1 where id=:id and empresa_id=:e and version=:v")
                .param("s",estado.name()).param("id",id).param("e",empresaId).param("v",version).update();
        if(n==0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return buscar(id,empresaId).orElseThrow();
    }
    @Override public void cancelarAlertas(UUID id, UUID empresaId) {
        jdbc.sql("update alertas.alertas set estado='CANCELADA',cancelada_at=now(),motivo_cancelacion='RECORDATORIO_CANCELADO',updated_at=now() where empresa_id=:e and metadata->>'recordatorioId'=:id and estado in('PROGRAMADA','PENDIENTE','ENVIADA','ATENDIDA','ERROR')")
                .param("e",empresaId).param("id",id.toString()).update();
    }
    private Recordatorio map(ResultSet rs,int row) throws SQLException {
        return new Recordatorio(rs.getObject("id",UUID.class),rs.getObject("empresa_id",UUID.class),
                rs.getObject("creado_por",UUID.class),rs.getString("titulo"),rs.getString("mensaje"),
                SeveridadAlerta.valueOf(rs.getString("severidad")),rs.getObject("animal_id",UUID.class),
                instant(rs,"fecha_evento"),instant(rs,"proxima_ejecucion"),rs.getInt("cantidad_notificaciones"),
                rs.getObject("intervalo_minutos",Integer.class),rs.getInt("notificaciones_generadas"),
                EstadoRecordatorio.valueOf(rs.getString("estado")),instant(rs,"created_at"),instant(rs,"updated_at"),rs.getLong("version"));
    }
    private Instant instant(ResultSet rs,String c)throws SQLException { OffsetDateTime v=rs.getObject(c,OffsetDateTime.class);return v==null?null:v.toInstant(); }
}
