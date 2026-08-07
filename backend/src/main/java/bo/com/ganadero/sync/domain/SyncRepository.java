package bo.com.ganadero.sync.domain;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface SyncRepository {
    Dispositivo upsertDispositivo(Dispositivo dispositivo);
    Optional<Dispositivo> findDispositivo(UUID empresa, String codigoDispositivo);
    Optional<OperacionSync> findOperacion(UUID empresa, UUID dispositivoId, UUID clienteId);
    Optional<OperacionSync> findOperacionByIdempotencyKey(UUID empresa, String idempotencyKey);
    OperacionSync saveOperacion(OperacionSync operacion);
    void setDispositivoOrigen(String codigoDispositivo, UUID dispositivoId);
    List<CambioSync> pullCambios(UUID empresa, long cursor, int size);
    boolean hasCambiosDespues(UUID empresa, long cursor);
    long ultimoCursor(UUID empresa);

    List<Map<String, Object>> bootstrapEmpresas(UUID empresa);
    List<Map<String, Object>> bootstrapPropiedades(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
    List<Map<String, Object>> bootstrapSectores(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
    List<Map<String, Object>> bootstrapPotreros(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
    List<Map<String, Object>> bootstrapLotes(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
    List<Map<String, Object>> bootstrapRazas(UUID empresa);
    List<Map<String, Object>> bootstrapCategorias(UUID empresa);
    List<Map<String, Object>> bootstrapTiposPasto(UUID empresa);
    List<Map<String, Object>> bootstrapAnimales(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
    List<Map<String, Object>> bootstrapIdentificadores(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
    List<Map<String, Object>> bootstrapPesajes(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
    List<Map<String, Object>> bootstrapMembresias(UUID empresa, boolean todas, java.util.Set<UUID> permitidas);
}
