package bo.com.ganadero.dashboard.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardResumen(
        long totalAnimales,
        long animalesEnPotrero,
        long lotesActivos,
        long potrerosActivos,
        Double pesoPromedioKg,
        Double gananciaPromedioKg,
        long pesajesUltimos7Dias,
        long movimientosUltimos7Dias,
        long animalesSinPesaje,
        List<Distribucion> animalesPorCategoria,
        List<Distribucion> animalesPorPotrero,
        List<Distribucion> animalesPorLote,
        List<PesajeReciente> pesajesRecientes,
        List<AlertaBasica> alertas,
        Instant generadoEn) {

    public record Distribucion(String nombre, long total) {
    }

    public record PesajeReciente(UUID id, UUID animalId, String animalCodigo, String animalNombre,
                                 LocalDate fecha, BigDecimal pesoKg) {
    }

    public record AlertaBasica(String tipo, String mensaje, String severidad, long total) {
    }
}
