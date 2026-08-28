package bo.com.ganadero.alertas.infrastructure;

import bo.com.ganadero.alertas.domain.EstadoRecordatorio;
import bo.com.ganadero.alertas.domain.Recordatorio;
import bo.com.ganadero.alertas.domain.RecordatorioRepository;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import bo.com.ganadero.shared.db.Rows;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcRecordatorioRepository implements RecordatorioRepository {
    private final JdbcClient jdbc;
    public JdbcRecordatorioRepository(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override public Recordatorio guardar(Recordatorio r) {
        jdbc.sql("""
                insert into recordatorio(id,creado_por,titulo,mensaje,severidad,animal_id,
                  fecha_evento,proxima_ejecucion,cantidad_notificaciones,intervalo_minutos,estado)
                values(:id,:u,:t,:m,:s,:a,:fe,:pe,:c,:i,'ACTIVO')
                """).param("id",r.id()).param("u",r.creadoPor())
                .param("t",r.titulo()).param("m",r.mensaje()).param("s",r.severidad().name())
                .param("a",r.animalId())
                .param("fe",r.fechaEvento().toString())
                .param("pe",r.proximaEjecucion().toString())
                .param("c",r.cantidadNotificaciones()).param("i",r.intervaloMinutos()).update();
        return buscar(r.id(), r.empresaId()).orElseThrow();
    }
    @Override public List<Recordatorio> listar(UUID empresaId) {
        return jdbc.sql("select * from recordatorio order by created_at desc")
                .query(this::map).list();
    }
    @Override public Optional<Recordatorio> buscar(UUID id, UUID empresaId) {
        return jdbc.sql("select * from recordatorio where id=:id")
                .param("id",id).query(this::map).optional();
    }
    @Override public List<Recordatorio> bloquearVencidos(Instant ahora, int limite) {
        return jdbc.sql("select * from recordatorio where estado='ACTIVO' and proxima_ejecucion<=:n order by proxima_ejecucion limit :l")
                .param("n",ahora.toString()).param("l",limite).query(this::map).list();
    }
    @Override public void registrarEjecucion(Recordatorio r, Instant siguiente, boolean completado) {
        jdbc.sql("update recordatorio set notificaciones_generadas=notificaciones_generadas+1,proxima_ejecucion=coalesce(:p,proxima_ejecucion),estado=case when :c then 'COMPLETADO' else estado end,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),version=version+1 where id=:id")
                .param("p",siguiente != null ? siguiente.toString() : null)
                .param("c",completado).param("id",r.id()).update();
    }
    @Override public Recordatorio cambiarEstado(UUID id, UUID empresaId, EstadoRecordatorio estado, long version) {
        int n=jdbc.sql("update recordatorio set estado=:s,updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),version=version+1 where id=:id and version=:v")
                .param("s",estado.name()).param("id",id).param("v",version).update();
        if(n==0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return buscar(id,empresaId).orElseThrow();
    }
    @Override public void cancelarAlertas(UUID id, UUID empresaId) {
        jdbc.sql("update alerta set estado='CANCELADA',cancelada_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'),motivo_cancelacion='RECORDATORIO_CANCELADO',updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now') where json_extract(metadata,'$.recordatorioId')=:id and estado in('PROGRAMADA','PENDIENTE','ENVIADA','ATENDIDA','ERROR')")
                .param("id",id.toString()).update();
    }
    private Recordatorio map(ResultSet rs,int row) throws SQLException {
        return new Recordatorio(Rows.uuid(rs,"id"),null,
                Rows.uuid(rs,"creado_por"),rs.getString("titulo"),rs.getString("mensaje"),
                SeveridadAlerta.valueOf(rs.getString("severidad")),Rows.uuid(rs,"animal_id"),
                Rows.instant(rs,"fecha_evento"),Rows.instant(rs,"proxima_ejecucion"),rs.getInt("cantidad_notificaciones"),
                rs.getObject("intervalo_minutos",Integer.class),rs.getInt("notificaciones_generadas"),
                EstadoRecordatorio.valueOf(rs.getString("estado")),Rows.instant(rs,"created_at"),Rows.instant(rs,"updated_at"),rs.getLong("version"));
    }
}
