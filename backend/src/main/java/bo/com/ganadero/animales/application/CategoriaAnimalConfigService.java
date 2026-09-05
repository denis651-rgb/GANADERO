package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Administración de rangos de categoría por edad (Mi finca -> Configuración general) y
 * reclasificación masiva de animales. Los rangos con clasificacionAutomatica=false (Buey y
 * futuras excepciones) quedan exentos de las validaciones de solape/hueco/rango-abierto porque
 * no participan del cálculo automático.
 */
@Service
public class CategoriaAnimalConfigService {
    private static final ZoneId BOLIVIA = ZoneId.of("America/La_Paz");
    private static final int TAMANO_LOTE = 500;
    private static final String PERMISO = "CONFIGURACION_EDITAR";

    private final CategoriaAnimalRepository categorias;
    private final AnimalRepository animales;
    private final HistorialCategoriaAnimalRepository historial;
    private final UserContext context;
    private final TransactionTemplate porAnimal;

    public CategoriaAnimalConfigService(CategoriaAnimalRepository categorias, AnimalRepository animales,
                                        HistorialCategoriaAnimalRepository historial, UserContext context,
                                        PlatformTransactionManager transactionManager) {
        this.categorias = categorias;
        this.animales = animales;
        this.historial = historial;
        this.context = context;
        this.porAnimal = new TransactionTemplate(transactionManager);
        this.porAnimal.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(readOnly = true)
    public List<CategoriaAnimal> listarTodas() {
        context.requirePermission(PERMISO);
        return categorias.findAllIncludingInactive();
    }

    @Transactional
    public CategoriaAnimal crear(RangoCategoriaCommand c) {
        context.requirePermission(PERMISO);
        validar(c, null);
        CategoriaAnimal nueva = new CategoriaAnimal(UUID.randomUUID(), null, c.codigo(), c.nombre(),
                c.sexoAplicable(), c.edadMinMeses(), c.edadMaxMeses(), c.descripcion(), true,
                c.clasificacionAutomatica(), c.ordenEvaluacion());
        return categorias.crear(nueva);
    }

    @Transactional
    public CategoriaAnimal actualizar(UUID id, RangoCategoriaCommand c) {
        context.requirePermission(PERMISO);
        CategoriaAnimal actual = buscar(id);
        validar(c, id);
        CategoriaAnimal editada = new CategoriaAnimal(id, null, actual.codigo(), c.nombre(), c.sexoAplicable(),
                c.edadMinMeses(), c.edadMaxMeses(), c.descripcion(), actual.activo(), c.clasificacionAutomatica(),
                c.ordenEvaluacion());
        return categorias.actualizar(editada);
    }

    @Transactional
    public void cambiarEstado(UUID id, boolean activo) {
        context.requirePermission(PERMISO);
        buscar(id);
        categorias.cambiarEstado(id, activo);
    }

    @Transactional
    public void eliminar(UUID id) {
        context.requirePermission(PERMISO);
        buscar(id);
        if (animales.existeAnimalConCategoria(id) || historial.existeParaCategoria(id)) {
            throw new BusinessException(ErrorCode.ANIMAL_CATEGORY_IN_USE);
        }
        categorias.eliminar(id);
    }

    /** Cuenta cuántos animales activos cambiarían de categoría si este rango se aplicara, sin persistir nada. */
    @Transactional(readOnly = true)
    public int simular(RangoCategoriaCommand c, UUID idExcluido) {
        context.requirePermission(PERMISO);
        if (!c.clasificacionAutomatica()) return 0;
        LocalDate hoy = LocalDate.now(BOLIVIA);
        UUID idCandidata = idExcluido != null ? idExcluido : UUID.randomUUID();
        CategoriaAnimal candidata = new CategoriaAnimal(idCandidata, null, "SIMULACION", c.nombre(),
                c.sexoAplicable(), c.edadMinMeses(), c.edadMaxMeses(), c.descripcion(), true, true, c.ordenEvaluacion());
        int cambiarian = 0;
        int offset = 0;
        List<Animal> pagina;
        do {
            pagina = animales.findParaReclasificacion(offset, TAMANO_LOTE);
            for (Animal a : pagina) {
                if (candidata.appliesTo(a.sexo(), a.fechaNacimiento(), hoy) && !idCandidata.equals(a.categoriaActualId())) {
                    cambiarian++;
                }
            }
            offset += TAMANO_LOTE;
        } while (pagina.size() == TAMANO_LOTE);
        return cambiarian;
    }

    /** Disparado por el usuario desde Mi finca ("Aplicar reclasificación ahora"). */
    public ResultadoReclasificacion reclasificarManual() {
        CurrentUser u = context.requirePermission(PERMISO);
        return reclasificar(u.userId());
    }

    /** Disparado por el proceso diario / al iniciar la aplicación (sin usuario autenticado). */
    public ResultadoReclasificacion reclasificarSistema() {
        return reclasificar(null);
    }

    private ResultadoReclasificacion reclasificar(UUID actor) {
        List<CategoriaAnimal> automaticas = categorias.findAllIncludingInactive().stream()
                .filter(CategoriaAnimal::activo)
                .filter(CategoriaAnimal::clasificacionAutomatica)
                .sorted(Comparator.comparingInt(CategoriaAnimal::ordenEvaluacion))
                .toList();
        Map<UUID, CategoriaAnimal> porId = new HashMap<>();
        categorias.findAllIncludingInactive().forEach(cat -> porId.put(cat.id(), cat));
        LocalDate hoy = LocalDate.now(BOLIVIA);
        int procesados = 0, actualizados = 0, omitidos = 0, errores = 0;
        int offset = 0;
        List<Animal> pagina;
        do {
            pagina = animales.findParaReclasificacion(offset, TAMANO_LOTE);
            for (Animal a : pagina) {
                procesados++;
                try {
                    CategoriaAnimal actual = porId.get(a.categoriaActualId());
                    if (actual != null && !actual.clasificacionAutomatica()) { omitidos++; continue; }
                    List<CategoriaAnimal> candidatas = automaticas.stream()
                            .filter(cat -> cat.appliesTo(a.sexo(), a.fechaNacimiento(), hoy)).toList();
                    if (candidatas.size() != 1) { omitidos++; continue; }
                    CategoriaAnimal nueva = candidatas.getFirst();
                    if (nueva.id().equals(a.categoriaActualId())) continue;
                    boolean aplicado = Boolean.TRUE.equals(porAnimal.execute(status -> {
                        animales.actualizarCategoria(a.id(), nueva.id(), actor);
                        Long edadDias = ChronoUnit.DAYS.between(a.fechaNacimiento(), hoy);
                        historial.crear(new HistorialCategoriaAnimal(UUID.randomUUID(), a.id(), a.categoriaActualId(),
                                nueva.id(), Instant.now(), HistorialCategoriaAnimal.AUTOMATICO,
                                "Reclasificación automática por edad.", actor, edadDias,
                                !a.fechaNacimientoEstimada(), nueva.id()));
                        return true;
                    }));
                    if (aplicado) actualizados++; else errores++;
                } catch (Exception ex) {
                    errores++;
                }
            }
            offset += TAMANO_LOTE;
        } while (pagina.size() == TAMANO_LOTE);
        return new ResultadoReclasificacion(procesados, actualizados, omitidos, errores);
    }

    private CategoriaAnimal buscar(UUID id) {
        return categorias.findAllIncludingInactive().stream().filter(c -> c.id().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_CATEGORY_NOT_FOUND));
    }

    private void validar(RangoCategoriaCommand c, UUID idExcluido) {
        if (c.edadMinMeses() != null && c.edadMinMeses() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La edad mínima no puede ser negativa.");
        }
        if (c.edadMaxMeses() != null && c.edadMaxMeses() < 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La edad máxima no puede ser negativa.");
        }
        int min = c.edadMinMeses() == null ? 0 : c.edadMinMeses();
        if (c.edadMaxMeses() != null && c.edadMaxMeses() < min) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La edad máxima debe ser mayor o igual a la mínima.");
        }
        if (!Set.of("MACHO", "HEMBRA", "AMBOS").contains(c.sexoAplicable())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "El sexo aplicable no es válido.");
        }
        if (!c.clasificacionAutomatica()) return;
        List<CategoriaAnimal> activasAutomaticas = categorias.findAllIncludingInactive().stream()
                .filter(CategoriaAnimal::activo).filter(CategoriaAnimal::clasificacionAutomatica)
                .filter(x -> idExcluido == null || !x.id().equals(idExcluido))
                .toList();
        for (String sexo : sexosAfectados(c.sexoAplicable())) {
            List<CategoriaAnimal> mismoSexo = activasAutomaticas.stream()
                    .filter(x -> solapaSexo(x.sexoAplicable(), sexo)).toList();
            if (c.edadMaxMeses() == null && mismoSexo.stream().anyMatch(x -> x.edadMaxMeses() == null)) {
                throw new BusinessException(ErrorCode.ANIMAL_CATEGORY_RANGE_MULTIPLE_OPEN);
            }
            for (CategoriaAnimal otra : mismoSexo) {
                if (solapaRango(min, c.edadMaxMeses(), otra.edadMinMeses() == null ? 0 : otra.edadMinMeses(), otra.edadMaxMeses())) {
                    throw new BusinessException(ErrorCode.ANIMAL_CATEGORY_RANGE_OVERLAP,
                            "El rango se superpone con \"" + otra.nombre() + "\".");
                }
            }
            if (!c.confirmarHueco()) {
                List<int[]> todos = new ArrayList<>();
                todos.add(new int[]{min, c.edadMaxMeses() == null ? Integer.MAX_VALUE : c.edadMaxMeses()});
                for (CategoriaAnimal otra : mismoSexo) {
                    todos.add(new int[]{otra.edadMinMeses() == null ? 0 : otra.edadMinMeses(),
                            otra.edadMaxMeses() == null ? Integer.MAX_VALUE : otra.edadMaxMeses()});
                }
                todos.sort(Comparator.comparingInt(x -> x[0]));
                for (int i = 1; i < todos.size(); i++) {
                    if (todos.get(i)[0] > todos.get(i - 1)[1] + 1) throw new BusinessException(ErrorCode.ANIMAL_CATEGORY_RANGE_GAP);
                }
            }
        }
    }

    private static List<String> sexosAfectados(String sexo) {
        return "AMBOS".equals(sexo) ? List.of("MACHO", "HEMBRA") : List.of(sexo);
    }

    private static boolean solapaSexo(String a, String b) {
        return "AMBOS".equals(a) || "AMBOS".equals(b) || a.equals(b);
    }

    private static boolean solapaRango(int minA, Integer maxA, int minB, Integer maxB) {
        int endA = maxA == null ? Integer.MAX_VALUE : maxA;
        int endB = maxB == null ? Integer.MAX_VALUE : maxB;
        return minA <= endB && minB <= endA;
    }
}
