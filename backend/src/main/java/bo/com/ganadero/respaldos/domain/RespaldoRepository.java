package bo.com.ganadero.respaldos.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RespaldoRepository {
    Respaldo crear(Respaldo respaldo);

    Optional<Respaldo> buscar(String nombreArchivo);

    List<Respaldo> listar();

    void actualizarEstado(String nombreArchivo, EstadoRespaldo estado, String ultimoError, Instant fechaCopiaExterna);

    void actualizarIntegridad(String nombreArchivo, IntegridadRespaldo integridad);

    void eliminar(String nombreArchivo);

    /** Cuenta cuántos respaldos tienen integridad VALIDA (para la guarda "no borrar el único válido"). */
    long contarValidos();
}
