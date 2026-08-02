package bo.com.ganadero.empresas.application;

import java.time.Instant;
import java.util.UUID;

public record EmpresaActualizadaEvent(UUID empresaId, UUID usuarioId, String entidadTipo,
                                      UUID entidadId, Instant fecha) {}
