package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.pesajes.domain.EstadoPesaje;
import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.pesajes.domain.PesajeRepository;
import bo.com.ganadero.pesajes.domain.TipoPeso;
import bo.com.ganadero.sanidad.domain.PlanSanitarioItem;
import bo.com.ganadero.sanidad.domain.TipoCalculoDosis;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Calcula la dosis a aplicar (sección 6). Para POR_PESO usa preferentemente el último peso
 * MEDIDO; si no hay ninguno, usa el último ESTIMADO (marcándolo explícitamente en el resultado
 * para que la UI lo muestre como tal). Nunca inventa un peso: si no hay ninguno, falla y exige
 * registrar uno antes de confirmar.
 */
@Service
public class DosisCalculadaService {
    private final PesajeRepository pesajes;

    public DosisCalculadaService(PesajeRepository pesajes) {
        this.pesajes = pesajes;
    }

    public CalculoDosis calcular(PlanSanitarioItem actividad, UUID animalId, UUID empresa) {
        if (actividad.dosisTipoCalculo() != TipoCalculoDosis.POR_PESO) {
            return new CalculoDosis(actividad.dosisCantidad(), null, null, null);
        }
        List<Pesaje> activos = pesajes.findByAnimal(animalId, empresa).stream()
                .filter(p -> p.estado() == EstadoPesaje.ACTIVO).toList();
        Pesaje medido = activos.stream().filter(p -> p.tipoPeso() == TipoPeso.MEDIDO).findFirst().orElse(null);
        Pesaje usado = medido != null ? medido
                : activos.stream().filter(p -> p.tipoPeso() == TipoPeso.ESTIMADO).findFirst().orElse(null);
        if (usado == null) {
            throw new BusinessException(ErrorCode.SANIDAD_PESO_REQUERIDO_PARA_DOSIS,
                    "El animal no tiene ningún peso registrado; registra un peso antes de calcular la dosis.");
        }
        BigDecimal referencia = actividad.dosisPesoReferenciaKg();
        BigDecimal cantidad = actividad.dosisCantidad();
        if (referencia == null || cantidad == null || referencia.signum() == 0) {
            return new CalculoDosis(cantidad, usado.pesoKg(), usado.tipoPeso(),
                    usado.fecha().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        }
        BigDecimal calculada = cantidad.multiply(usado.pesoKg())
                .divide(referencia, 3, RoundingMode.HALF_UP);
        if (actividad.dosisMinima() != null && calculada.compareTo(actividad.dosisMinima()) < 0) {
            calculada = actividad.dosisMinima();
        }
        if (actividad.dosisMaxima() != null && calculada.compareTo(actividad.dosisMaxima()) > 0) {
            calculada = actividad.dosisMaxima();
        }
        return new CalculoDosis(calculada, usado.pesoKg(), usado.tipoPeso(),
                usado.fecha().atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
    }
}
