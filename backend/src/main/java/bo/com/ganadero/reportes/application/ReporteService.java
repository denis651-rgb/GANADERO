package bo.com.ganadero.reportes.application;

import bo.com.ganadero.reportes.domain.ReporteAnimalMuerto;
import bo.com.ganadero.reportes.domain.ReporteAnimalNacido;
import bo.com.ganadero.reportes.domain.ReporteRepository;
import bo.com.ganadero.reportes.domain.ReporteVenta;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReporteService {
    private final ReporteRepository reportes;
    private final UserContext context;

    public ReporteService(ReporteRepository reportes, UserContext context) {
        this.reportes = reportes;
        this.context = context;
    }

    @Transactional(readOnly = true)
    public List<ReporteAnimalNacido> nacimientos(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return reportes.nacidos(desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<ReporteAnimalMuerto> muertes(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return reportes.muertos(desde, hasta);
    }

    @Transactional(readOnly = true)
    public List<ReporteVenta> ventas(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return reportes.ventas(desde, hasta);
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        context.requirePermission("REPORTE_VER");
        if (desde == null || hasta == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        if (desde.isAfter(hasta)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
    }
}
