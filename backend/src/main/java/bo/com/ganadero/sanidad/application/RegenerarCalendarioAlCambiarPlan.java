package bo.com.ganadero.sanidad.application;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sin esto, una actividad recién creada no aparecía en el Calendario hasta la corrida de las 00:10
 * (o hasta sincronizar con Google), y una app de escritorio casi nunca está abierta a esa hora.
 *
 * <p>Corre <b>después</b> de confirmar el cambio y en un hilo aparte, por dos razones: (1) si la
 * generación fallara, la actividad ya guardada no se deshace; la corrida del arranque y la
 * nocturna lo reintentan; (2) la app de escritorio usa un pool de <b>una sola conexión</b>
 * ({@code application-local.yml}), así que abrir aquí una transacción nueva mientras la del
 * guardado sigue ocupando esa conexión dejaría la petición colgada hasta agotar el tiempo de
 * espera. En otro hilo, la generación simplemente espera a que la conexión quede libre.</p>
 *
 * <p>Un solo hilo: varios cambios seguidos se ponen en cola y nunca corren dos generaciones a la vez.</p>
 */
@Component
class RegenerarCalendarioAlCambiarPlan {
    private static final Logger LOG = LoggerFactory.getLogger(RegenerarCalendarioAlCambiarPlan.class);

    private final CalendarioSanitarioService calendario;
    private final ExecutorService ejecutor = Executors.newSingleThreadExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "calendario-sanitario");
        hilo.setDaemon(true);
        return hilo;
    });

    RegenerarCalendarioAlCambiarPlan(CalendarioSanitarioService calendario) {
        this.calendario = calendario;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void alCambiarElPlan(PlanSanitarioModificado evento) {
        ejecutor.execute(this::generar);
    }

    private void generar() {
        try {
            calendario.procesar();
        } catch (RuntimeException e) {
            LOG.warn("No se pudo generar el calendario sanitario tras el cambio del plan; se reintentará en la próxima corrida.", e);
        }
    }

    @PreDestroy
    void detener() {
        ejecutor.shutdown();
    }
}
