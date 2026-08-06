package bo.com.ganadero.auditoria.domain;

import java.util.List;
import java.util.UUID;

public interface AuditoriaRepository {
    void insert(AuditoriaRegistro registro);
    AuditPage findAll(UUID empresa, AuditoriaFilter filter);
    List<AuditoriaRegistro> findLast(UUID empresa, UUID entidadId, String modulo, String entidad, int limit);
}
