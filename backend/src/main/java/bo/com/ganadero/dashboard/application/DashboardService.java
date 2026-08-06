package bo.com.ganadero.dashboard.application;

import bo.com.ganadero.dashboard.domain.DashboardRepository;
import bo.com.ganadero.dashboard.domain.DashboardResumen;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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
        return new DashboardResumen(
                repository.countAnimales(empresa, todas, permitidas),
                repository.countAnimalesEnPotrero(empresa, todas, permitidas),
                repository.countLotesActivos(empresa, todas, permitidas),
                repository.countPotrerosActivos(empresa, todas, permitidas),
                repository.pesoPromedio(empresa, todas, permitidas),
                repository.gananciaDiaria(empresa, todas, permitidas),
                repository.countPesajesUltimosDias(empresa, 7),
                repository.countMovimientosUltimosDias(empresa, 7),
                repository.countAnimalesSinPesaje(empresa, todas, permitidas),
                Instant.now());
    }
}
