package bo.com.ganadero.sync.domain;

/**
 * Estados canónicos de una operación local (Tarea 10.1).
 *
 * <p>Son los únicos estados permitidos en {@code sync.operaciones.estado} y
 * se mapean de forma idéntica a la API y al cliente:</p>
 * <ul>
 *   <li>{@code PENDING}: esperando ser procesada.</li>
 *   <li>{@code PROCESSING}: en ejecución.</li>
 *   <li>{@code SYNCED}: aplicada en el servidor.</li>
 *   <li>{@code CONFLICT}: choque de versiones o idempotencia.</li>
 *   <li>{@code REJECTED}: rechazada de forma definitiva (regla de negocio).</li>
 *   <li>{@code RETRYABLE}: error transitorio; puede reintentarse.</li>
 * </ul>
 */
public enum EstadoOperacionSync {
    PENDING, PROCESSING, SYNCED, CONFLICT, REJECTED, RETRYABLE
}
