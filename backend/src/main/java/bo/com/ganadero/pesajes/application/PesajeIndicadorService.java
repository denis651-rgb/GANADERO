package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.pesajes.domain.EstadoPesaje;
import bo.com.ganadero.pesajes.domain.EvolucionPesaje;
import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.pesajes.domain.PesajeIndicadorAnimal;
import bo.com.ganadero.pesajes.domain.PesajeIndicadorLote;
import bo.com.ganadero.pesajes.domain.PesajeRepository;
import bo.com.ganadero.pesajes.domain.PesajeSinPesaje;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PesajeIndicadorService {
    private final PesajeRepository pesajes;
    private final AnimalRepository animales;
    private final LoteRepository lotes;
    private final UserContext context;

    public PesajeIndicadorService(PesajeRepository pesajes, AnimalRepository animales,
                                  LoteRepository lotes, UserContext context) {
        this.pesajes = pesajes;
        this.animales = animales;
        this.lotes = lotes;
        this.context = context;
    }

    @Transactional(readOnly = true)
    public PesajeIndicadorAnimal indicadorAnimal(UUID animalId) {
        CurrentUser user = context.requirePermission("PESAJE_VER");
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());

        List<Pesaje> activos = pesajes.findByAnimal(animalId, user.empresaId()).stream()
                .filter(p -> p.estado() == EstadoPesaje.ACTIVO)
                .toList();
        Pesaje ultimo = activos.isEmpty() ? null : activos.get(0);
        Pesaje anterior = activos.size() < 2 ? null : activos.get(1);

        BigDecimal variacionKg = null;
        BigDecimal variacionPct = null;
        BigDecimal gananciaDiariaKg = null;
        if (ultimo != null && anterior != null) {
            variacionKg = ultimo.pesoKg().subtract(anterior.pesoKg());
            if (anterior.pesoKg().signum() != 0) {
                variacionPct = variacionKg.multiply(new BigDecimal("100"))
                        .divide(anterior.pesoKg(), 2, RoundingMode.HALF_UP);
            }
            gananciaDiariaKg = gananciaDiaria(ultimo.fecha(), ultimo.pesoKg(), anterior.fecha(), anterior.pesoKg());
        }

        UUID loteId = animal.loteActualId();
        BigDecimal promedioLote = null;
        Integer animalesPesadosLote = null;
        if (loteId != null) {
            PesajeIndicadorLote indicador = pesajes.indicadorLote(loteId, user.empresaId()).orElse(null);
            if (indicador != null) {
                promedioLote = indicador.pesoPromedioKg();
                animalesPesadosLote = indicador.animalesPesados();
            }
        }

        BigDecimal diferenciaVsLote = null;
        BigDecimal diferenciaVsLotePct = null;
        if (ultimo != null && promedioLote != null) {
            diferenciaVsLote = ultimo.pesoKg().subtract(promedioLote);
            if (promedioLote.signum() != 0) {
                diferenciaVsLotePct = diferenciaVsLote.multiply(new BigDecimal("100"))
                        .divide(promedioLote, 2, RoundingMode.HALF_UP);
            }
        }

        List<EvolucionPesaje> evolucion = new ArrayList<>();
        for (int i = activos.size() - 1; i >= 0; i--) {
            Pesaje p = activos.get(i);
            evolucion.add(new EvolucionPesaje(p.fecha(), p.pesoKg(), p.condicionCorporal(), p.tipo()));
        }

        return new PesajeIndicadorAnimal(animalId, animal.codigo(), animal.nombre(),
                ultimo == null ? null : ultimo.pesoKg(),
                ultimo == null ? null : ultimo.fecha(),
                anterior == null ? null : anterior.pesoKg(),
                anterior == null ? null : anterior.fecha(),
                variacionKg, variacionPct, gananciaDiariaKg,
                promedioLote, animalesPesadosLote, diferenciaVsLote, diferenciaVsLotePct, evolucion);
    }

    @Transactional(readOnly = true)
    public PesajeIndicadorLote indicadorLote(UUID loteId) {
        CurrentUser user = context.requirePermission("PESAJE_VER");
        Lote lote = lotes.findById(loteId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
        context.requirePropertyAccess(user, lote.propiedadId());
        return pesajes.indicadorLote(loteId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<PesajeSinPesaje> animalesSinPesaje(int page, int size) {
        CurrentUser user = context.requirePermission("PESAJE_VER");
        return pesajes.animalesSinPesaje(user.empresaId(), user.accesoTodasPropiedades(),
                user.propiedadesPermitidas(), page, size);
    }

    @Transactional(readOnly = true)
    public long countAnimalesSinPesaje() {
        CurrentUser user = context.requirePermission("PESAJE_VER");
        return pesajes.countAnimalesSinPesaje(user.empresaId(), user.accesoTodasPropiedades(),
                user.propiedadesPermitidas());
    }

    private BigDecimal gananciaDiaria(LocalDate fechaUltimo, BigDecimal pesoUltimo,
                                      LocalDate fechaAnterior, BigDecimal pesoAnterior) {
        long dias = ChronoUnit.DAYS.between(fechaAnterior, fechaUltimo);
        if (dias <= 0) return BigDecimal.ZERO;
        return pesoUltimo.subtract(pesoAnterior).divide(BigDecimal.valueOf(dias), 3, RoundingMode.HALF_UP);
    }
}
