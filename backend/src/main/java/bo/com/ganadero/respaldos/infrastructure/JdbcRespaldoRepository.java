package bo.com.ganadero.respaldos.infrastructure;

import bo.com.ganadero.respaldos.domain.EstadoRespaldo;
import bo.com.ganadero.respaldos.domain.IntegridadRespaldo;
import bo.com.ganadero.respaldos.domain.Respaldo;
import bo.com.ganadero.respaldos.domain.RespaldoRepository;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcRespaldoRepository implements RespaldoRepository {
    private final JdbcClient jdbc;

    public JdbcRespaldoRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Respaldo crear(Respaldo r) {
        jdbc.sql("""
                insert into respaldo_registro(nombre_archivo,fecha_creacion,tamano_bytes,hash_sha256,
                    version_formato,version_aplicacion,version_base_datos,empresa_id,estado,integridad,
                    ultimo_error,fecha_copia_externa,created_by)
                values(:nombre,:fecha,:tamano,:hash,:versionFormato,:versionApp,:versionDb,:empresa,
                    :estado,:integridad,:error,:fechaCopia,:actor)
                """)
                .param("nombre", r.nombreArchivo())
                .param("fecha", r.fechaCreacion().toString())
                .param("tamano", r.tamanoBytes())
                .param("hash", r.hashSha256())
                .param("versionFormato", r.versionFormato())
                .param("versionApp", r.versionAplicacion())
                .param("versionDb", r.versionBaseDatos())
                .param("empresa", r.empresaId() == null ? null : r.empresaId().toString())
                .param("estado", r.estado().name())
                .param("integridad", r.integridad().name())
                .param("error", r.ultimoError())
                .param("fechaCopia", r.fechaCopiaExterna() == null ? null : r.fechaCopiaExterna().toString())
                .param("actor", r.createdBy() == null ? null : r.createdBy().toString())
                .update();
        return buscar(r.nombreArchivo()).orElseThrow();
    }

    @Override
    public Optional<Respaldo> buscar(String nombreArchivo) {
        return jdbc.sql("select * from respaldo_registro where nombre_archivo=:nombre")
                .param("nombre", nombreArchivo).query(this::map).optional();
    }

    @Override
    public List<Respaldo> listar() {
        return jdbc.sql("select * from respaldo_registro order by fecha_creacion desc").query(this::map).list();
    }

    @Override
    public void actualizarEstado(String nombreArchivo, EstadoRespaldo estado, String ultimoError, Instant fechaCopiaExterna) {
        jdbc.sql("update respaldo_registro set estado=:estado, ultimo_error=:error, fecha_copia_externa=coalesce(:fecha,fecha_copia_externa) where nombre_archivo=:nombre")
                .param("estado", estado.name())
                .param("error", ultimoError)
                .param("fecha", fechaCopiaExterna == null ? null : fechaCopiaExterna.toString())
                .param("nombre", nombreArchivo)
                .update();
    }

    @Override
    public void actualizarIntegridad(String nombreArchivo, IntegridadRespaldo integridad) {
        jdbc.sql("update respaldo_registro set integridad=:integridad where nombre_archivo=:nombre")
                .param("integridad", integridad.name()).param("nombre", nombreArchivo).update();
    }

    @Override
    public void eliminar(String nombreArchivo) {
        jdbc.sql("delete from respaldo_registro where nombre_archivo=:nombre").param("nombre", nombreArchivo).update();
    }

    @Override
    public long contarValidos() {
        return jdbc.sql("select count(*) from respaldo_registro where integridad='VALIDA'").query(Long.class).single();
    }

    private Respaldo map(ResultSet r, int row) throws SQLException {
        String empresaId = r.getString("empresa_id");
        String createdBy = r.getString("created_by");
        return new Respaldo(r.getString("nombre_archivo"), Rows.instant(r, "fecha_creacion"),
                r.getLong("tamano_bytes"), r.getString("hash_sha256"), r.getInt("version_formato"),
                r.getString("version_aplicacion"), r.getString("version_base_datos"),
                empresaId == null ? null : java.util.UUID.fromString(empresaId),
                EstadoRespaldo.valueOf(r.getString("estado")), IntegridadRespaldo.valueOf(r.getString("integridad")),
                r.getString("ultimo_error"), Rows.instant(r, "fecha_copia_externa"),
                createdBy == null ? null : java.util.UUID.fromString(createdBy), Rows.instant(r, "created_at"));
    }
}
