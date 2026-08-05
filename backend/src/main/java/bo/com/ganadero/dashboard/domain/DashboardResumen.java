package bo.com.ganadero.dashboard.domain;

import java.time.Instant;

public record DashboardResumen(
        long totalAnimales,
        long animalesEnPotrero,
        long lotesActivos,
        long potrerosActivos,
        Double pesoPromedioKg,
        Double gananciaDiariaKg,
        long pesajesUltimos7Dias,
        long movimientosUltimos7Dias,
        long animalesSinPesaje,
        Instant generadoEn) {
}
