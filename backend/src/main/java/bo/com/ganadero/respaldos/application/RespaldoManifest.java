package bo.com.ganadero.respaldos.application;

/** Contenido de manifest.json dentro de un .ganadero-backup. Nunca incluye tokens/credenciales/logs. */
public record RespaldoManifest(String formato, int versionFormato, String versionAplicacion,
                               String versionBaseDatos, String fechaCreacion, String zonaHoraria,
                               String empresaId, String archivoInterno, long tamanoBytes, String hashSha256) {
}
