package bo.com.ganadero.shared.audit;

/**
 * Catálogo de acciones de auditoría de la Fase 3 (Bloque 35 ampliado).
 *
 * <p>Los nombres de acción son strings libres en {@code auditoria.registros};
 * esta clase los centraliza para mantener consistencia entre los módulos.</p>
 */
public final class AuditActions {

    private AuditActions() {
    }

    // Reproducción
    public static final String REGISTRAR_CELO = "REGISTRAR_CELO";
    public static final String ANULAR_CELO = "ANULAR_CELO";
    public static final String REGISTRAR_SERVICIO = "REGISTRAR_SERVICIO";
    public static final String REGISTRAR_DIAGNOSTICO = "REGISTRAR_DIAGNOSTICO";
    public static final String REGISTRAR_PARTO = "REGISTRAR_PARTO";
    public static final String REGISTRAR_DESTETE = "REGISTRAR_DESTETE";

    // Sanidad
    public static final String CREAR_PLAN_SANITARIO = "CREAR_PLAN_SANITARIO";
    public static final String CONFIRMAR_JORNADA_SANITARIA = "CONFIRMAR_JORNADA_SANITARIA";
    public static final String REGISTRAR_CASO_CLINICO = "REGISTRAR_CASO_CLINICO";
    public static final String CREAR_TRATAMIENTO = "CREAR_TRATAMIENTO";
    public static final String APLICAR_TRATAMIENTO = "APLICAR_TRATAMIENTO";
    public static final String INICIAR_CUARENTENA = "INICIAR_CUARENTENA";
    public static final String FINALIZAR_CUARENTENA = "FINALIZAR_CUARENTENA";

    // Alertas
    public static final String GENERAR_ALERTA = "GENERAR_ALERTA";
    public static final String RESOLVER_ALERTA = "RESOLVER_ALERTA";
    public static final String DESCARTAR_ALERTA = "DESCARTAR_ALERTA";
}
