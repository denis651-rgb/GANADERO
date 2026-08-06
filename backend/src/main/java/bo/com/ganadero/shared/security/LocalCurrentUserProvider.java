package bo.com.ganadero.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.security", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LocalCurrentUserProvider implements CurrentUserProvider {
    private final CurrentUser currentUser;

    public LocalCurrentUserProvider(
            @Value("${app.security.local-user-id:00000000-0000-0000-0000-000000000001}") UUID userId,
            @Value("${app.security.local-empresa-id:00000000-0000-0000-0000-000000000001}") UUID empresaId,
            @Value("${app.security.local-member-id:00000000-0000-0000-0000-000000000001}") UUID memberId) {
        currentUser = new CurrentUser(userId, empresaId, memberId, Set.of("PROPIETARIO"),
                Set.of(
                        "EMPRESA_VER", "EMPRESA_EDITAR", "CONFIGURACION_EMPRESA_VER", "CONFIGURACION_EMPRESA_EDITAR",
                        "USUARIO_VER", "USUARIO_CREAR", "USUARIO_EDITAR", "USUARIO_BLOQUEAR", "USUARIO_ASIGNAR_ROL",
                        "ROL_VER", "ROL_CREAR", "ROL_EDITAR", "ROL_ASIGNAR_PERMISOS",
                        "PROPIEDAD_VER", "PROPIEDAD_CREAR", "PROPIEDAD_EDITAR",
                        "POTRERO_VER", "POTRERO_CREAR", "POTRERO_EDITAR",
                        "ANIMAL_VER", "ANIMAL_CREAR", "ANIMAL_EDITAR", "ANIMAL_CAMBIAR_ESTADO", "ANIMAL_REGISTRAR_BAJA",
                        "IDENTIFICADOR_VER", "IDENTIFICADOR_ASIGNAR", "IDENTIFICADOR_RETIRAR",
                        "LOTE_VER", "LOTE_CREAR", "LOTE_EDITAR", "LOTE_ASIGNAR_ANIMALES",
                        "MOVIMIENTO_VER", "MOVIMIENTO_CREAR", "MOVIMIENTO_CONFIRMAR", "MOVIMIENTO_ANULAR", "AUDITORIA_VER",
                        "PESAJE_VER", "PESAJE_REGISTRAR", "PESAJE_ANULAR",
                        "SINC_DISPOSITIVO_REGISTRAR", "SINC_PUSH", "SINC_PULL", "SINC_BOOTSTRAP",
                        "DOCUMENTO_VER", "DOCUMENTO_SUBIR", "DOCUMENTO_ELIMINAR", "DASHBOARD_VER",
                        "SISTEMA_ESTADO_VER"
                ),
                Set.of(), true);
    }

    @Override public CurrentUser get() { return currentUser; }
}
