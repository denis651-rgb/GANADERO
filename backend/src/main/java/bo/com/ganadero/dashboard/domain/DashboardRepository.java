package bo.com.ganadero.dashboard.domain;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface DashboardRepository {
    long countAnimales(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countAnimalesEnPotrero(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countLotesActivos(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countPotrerosActivos(UUID empresa, boolean todas, Set<UUID> permitidas);

    Double pesoPromedio(UUID empresa, boolean todas, Set<UUID> permitidas);

    Double gananciaDiaria(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countPesajesUltimosDias(UUID empresa, int dias, boolean todas, Set<UUID> permitidas);

    long countMovimientosUltimosDias(UUID empresa, int dias, boolean todas, Set<UUID> permitidas);

    long countAnimalesSinPesaje(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countAnimalesGananciaNegativa(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countPotrerosInactivos(UUID empresa, boolean todas, Set<UUID> permitidas);

    long countLotesCerrados(UUID empresa, boolean todas, Set<UUID> permitidas);

    List<DashboardResumen.Distribucion> animalesPorCategoria(UUID empresa, boolean todas, Set<UUID> permitidas);

    List<DashboardResumen.Distribucion> animalesPorPotrero(UUID empresa, boolean todas, Set<UUID> permitidas);

    List<DashboardResumen.Distribucion> animalesPorLote(UUID empresa, boolean todas, Set<UUID> permitidas);

    List<DashboardResumen.PesajeReciente> pesajesRecientes(UUID empresa, boolean todas, Set<UUID> permitidas, int limite);
}
