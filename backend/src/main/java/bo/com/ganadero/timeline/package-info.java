/**
 * Módulo timeline: línea de tiempo estandarizada del animal.
 *
 * <p>Expone la escritura de eventos ({@code TimelineEventPublisher}) y la
 * consulta paginada canónica. No depende de ningún módulo de negocio; los
 * demás módulos dependen de él para publicar eventos en la misma transacción.</p>
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package bo.com.ganadero.timeline;
