package bo.com.ganadero.dashboard.application;

import bo.com.ganadero.dashboard.domain.DashboardRepository;
import bo.com.ganadero.dashboard.domain.DashboardResumen;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DashboardService {
    private final DashboardRepository repository;
    private final UserContext context;

    public DashboardService(DashboardRepository repository, UserContext context) {
        this.repository = repository;
        this.context = context;
    }

    @Transactional(readOnly = true)
    public DashboardResumen resumen() {
        CurrentUser user = context.requirePermission("DASHBOARD_VER");
        UUID empresa = user.empresaId();
        boolean todas = user.accesoTodasPropiedades();
        var permitidas = user.propiedadesPermitidas();

        long sinPesaje = repository.countAnimalesSinPesaje(empresa, todas, permitidas);
        return new DashboardResumen(
                repository.countAnimales(empresa, todas, permitidas),
                repository.countAnimalesEnPotrero(empresa, todas, permitidas),
                repository.countLotesActivos(empresa, todas, permitidas),
                repository.countPotrerosActivos(empresa, todas, permitidas),
                repository.pesoPromedio(empresa, todas, permitidas),
                repository.gananciaDiaria(empresa, todas, permitidas),
                repository.countPesajesUltimosDias(empresa, 7, todas, permitidas),
                repository.countMovimientosUltimosDias(empresa, 7, todas, permitidas),
                sinPesaje,
                repository.animalesPorCategoria(empresa, todas, permitidas),
                repository.animalesPorPotrero(empresa, todas, permitidas),
                repository.animalesPorLote(empresa, todas, permitidas),
                repository.pesajesRecientes(empresa, todas, permitidas, 8),
                alertas(empresa, todas, permitidas, sinPesaje),
                Instant.now());
    }

    private List<DashboardResumen.AlertaBasica> alertas(UUID empresa, boolean todas, Set<UUID> permitidas, long sinPesaje) {
        List<DashboardResumen.AlertaBasica> alertas = new ArrayList<>();
        if (sinPesaje > 0) {
            alertas.add(new DashboardResumen.AlertaBasica("SIN_PESAJE", "Animales sin pesaje hace más de 30 días", "warning", sinPesaje));
        }
        long gananciaNegativa = repository.countAnimalesGananciaNegativa(empresa, todas, permitidas);
        if (gananciaNegativa > 0) {
            alertas.add(new DashboardResumen.AlertaBasica("GANANCIA_NEGATIVA", "Animales con ganancia diaria negativa", "danger", gananciaNegativa));
        }
        long potrerosInactivos = repository.countPotrerosInactivos(empresa, todas, permitidas);
        if (potrerosInactivos > 0) {
            alertas.add(new DashboardResumen.AlertaBasica("POTREROS_INACTIVOS", "Potreros inactivos", "info", potrerosInactivos));
        }
        long lotesCerrados = repository.countLotesCerrados(empresa, todas, permitidas);
        if (lotesCerrados > 0) {
            alertas.add(new DashboardResumen.AlertaBasica("LOTES_CERRADOS", "Lotes cerrados", "info", lotesCerrados));
        }
        return alertas;
    }
}
