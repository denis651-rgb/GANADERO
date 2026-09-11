package bo.com.ganadero.respaldos.domain;

import java.time.Instant;
import java.util.UUID;

public record Respaldo(String nombreArchivo, Instant fechaCreacion, long tamanoBytes, String hashSha256,
                       int versionFormato, String versionAplicacion, String versionBaseDatos, UUID empresaId,
                       EstadoRespaldo estado, IntegridadRespaldo integridad, String ultimoError,
                       Instant fechaCopiaExterna, UUID createdBy, Instant createdAt) {
}
