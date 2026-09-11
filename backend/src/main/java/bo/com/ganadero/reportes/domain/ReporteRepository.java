package bo.com.ganadero.reportes.domain;

import java.time.LocalDate;
import java.util.List;

public interface ReporteRepository {
    List<ReporteAnimalNacido> nacidos(LocalDate desde, LocalDate hasta);

    List<ReporteAnimalMuerto> muertos(LocalDate desde, LocalDate hasta);

    List<ReporteVenta> ventas(LocalDate desde, LocalDate hasta);
}
