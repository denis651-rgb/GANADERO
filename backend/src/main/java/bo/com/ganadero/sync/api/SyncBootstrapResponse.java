package bo.com.ganadero.sync.api;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public record SyncBootstrapResponse(
        UUID dispositivoId,
        long cursor,
        Map<String, Object> empresa,
        List<Map<String, Object>> propiedades,
        List<Map<String, Object>> sectores,
        List<Map<String, Object>> potreros,
        List<Map<String, Object>> lotes,
        List<Map<String, Object>> razas,
        List<Map<String, Object>> categorias,
        List<Map<String, Object>> tiposPasto,
        List<Map<String, Object>> animales,
        List<Map<String, Object>> identificadores,
        List<Map<String, Object>> pesajes,
        List<Map<String, Object>> membresias,
        SyncUsuarioInfo usuario) {
}
