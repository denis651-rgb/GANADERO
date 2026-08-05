package bo.com.ganadero.dashboard.domain;

import java.util.Set;
import java.util.UUID;

public interface DashboardRepository {
    long countAnimales(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countAnimalesEnPotrero(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countLotesActivos(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countPotrerosActivos(UUID empresa, boolean todas, Set<UUID> permitidas);

    Double pesoPromedio(UUID empresa, boolean todas, Set<UUID> permitidas);

    Double gananciaDiaria(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countPesajesUltimosDias(UUID empresa, int dias);

    long countMovimientosUltimosDias(UUID empresa, int dias);

    long countAnimalesSinPesaje(UUID empresa, boolean todas, Set<UUID> permitidas);
}
