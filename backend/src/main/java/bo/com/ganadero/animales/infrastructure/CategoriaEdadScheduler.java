package bo.com.ganadero.animales.infrastructure;

import bo.com.ganadero.animales.application.CategoriaAnimalConfigService;
import bo.com.ganadero.animales.application.ResultadoReclasificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Component
class CategoriaEdadScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(CategoriaEdadScheduler.class);
    private final CategoriaAnimalConfigService categorias;

    CategoriaEdadScheduler(CategoriaAnimalConfigService categorias) {
        this.categorias = categorias;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void sincronizarAlIniciar() {
        sincronizarAhora();
    }

    @Scheduled(cron = "${ganadero.animales.cron-categorias:0 20 0 * * *}", zone = "America/La_Paz")
    public void sincronizar() {
        sincronizarAhora();
    }

    private void sincronizarAhora() {
        ResultadoReclasificacion r = categorias.reclasificarSistema();
        if (r.actualizados() > 0) {
            LOG.info("Categorías actualizadas automáticamente por edad: {} (procesados {}, omitidos {}, errores {})",
                    r.actualizados(), r.procesados(), r.omitidos(), r.errores());
        }
        if (r.errores() > 0) {
            LOG.warn("Reclasificación automática por edad terminó con {} error(es).", r.errores());
        }
    }
}
