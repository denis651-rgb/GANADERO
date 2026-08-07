package bo.com.ganadero.timeline.application;

/**
 * Punto único de escritura de eventos de la línea de tiempo (Bloque 26).
 *
 * <p>La publicación se ejecuta dentro de la transacción del llamante
 * (Bloque 30) y es idempotente mediante la clave del evento (Bloque 27).</p>
 */
public interface TimelineEventPublisher {

    void publish(RegistrarEventoTimeline evento);
}
