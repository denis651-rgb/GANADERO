package bo.com.ganadero.sanidad.application;
import bo.com.ganadero.alertas.application.*;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.error.*;
import bo.com.ganadero.shared.security.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

/**
 * Historial sanitario declarado al ingreso (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md,
 * secciones 3.2 y 7): registra lo que el vendedor certifica sobre un animal comprado —
 * AplicacionSanitaria sin jornada asociada, origenRegistro=DECLARADA_PROVEEDOR. Vive aparte
 * de JornadaSanitariaService porque es un flujo distinto (sin jornada, sin selección de
 * elegibilidad), aunque reusa el mismo JornadaSanitariaRepository porque ahí vive
 * AplicacionSanitaria.
 */
@Service
public class HistorialDeclaradoService {
    private final JornadaSanitariaRepository repo;
    private final SanidadRepository planes;
    private final AnimalRepository animales;
    private final UserContext context;
    private final ObjectProvider<MotorAlertas> alertas;
    private final ApplicationEventPublisher events;

    public HistorialDeclaradoService(JornadaSanitariaRepository repo, SanidadRepository planes,
                                     AnimalRepository animales, UserContext context,
                                     ObjectProvider<MotorAlertas> alertas, ApplicationEventPublisher events) {
        this.repo = repo;
        this.planes = planes;
        this.animales = animales;
        this.context = context;
        this.alertas = alertas;
        this.events = events;
    }

    @Transactional
    public AplicacionSanitaria registrar(RegistrarAplicacionDeclaradaCommand c) {
        CurrentUser u = context.requirePermission("SANIDAD_JORNADA_CONFIRMAR");
        return registrar(c, u);
    }

    @Transactional
    public List<AplicacionSanitaria> registrarLote(RegistrarHistorialDeclaradoLoteCommand command) {
        CurrentUser u = context.requirePermission("SANIDAD_JORNADA_CONFIRMAR");
        List<UUID> animalIds = command.animalIds();
        List<RegistrarHistorialDeclaradoLoteCommand.Actividad> actividades = command.actividades();
        if (new HashSet<>(animalIds).size() != animalIds.size()) {
            throw new BusinessException(ErrorCode.DUPLICATE_ANIMAL_IN_REQUEST);
        }
        Set<String> actividadesUnicas = new HashSet<>();
        for (var actividad : actividades) {
            String clave = actividad.tipoActividad() + "|" + actividad.planItemId() + "|"
                    + actividad.fechaAplicacion() + "|" + Objects.toString(actividad.productoTexto(), "");
            if (!actividadesUnicas.add(clave)) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                        "La declaración contiene actividades sanitarias duplicadas.");
            }
        }
        List<AplicacionSanitaria> resultado = new ArrayList<>(animalIds.size() * actividades.size());
        for (UUID animalId : animalIds) {
            for (var actividad : actividades) {
                resultado.add(registrar(new RegistrarAplicacionDeclaradaCommand(animalId,
                        actividad.tipoActividad(), actividad.planItemId(), actividad.fechaAplicacion(),
                        actividad.dosis(), actividad.unidadDosis(), actividad.productoTexto(),
                        actividad.observaciones()), u));
            }
        }
        return List.copyOf(resultado);
    }

    private AplicacionSanitaria registrar(RegistrarAplicacionDeclaradaCommand c, CurrentUser u) {
        Animal animal = animales.findById(c.animalId(), u.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(u, animal.propiedadActualId());
        ReglasSanitarias.fechaAnimal(animal, c.fechaAplicacion(), true);
        ReglasSanitarias.exigir(animal.fechaIngreso() == null || !c.fechaAplicacion().isAfter(animal.fechaIngreso()),
                "El antecedente del proveedor no puede ser posterior al ingreso del animal.");
        PlanSanitarioItem item = c.planItemId() == null ? null
                : requireItemCompatible(c.planItemId(), u.empresaId(), c.tipoActividad());
        if (item != null) ReglasSanitarias.intervaloMinimoEntreAplicaciones(repo, u.empresaId(), animal.id(), item, item.productoId(), c.fechaAplicacion());
        LocalDate proxima = item != null && item.frecuenciaDias() != null
                ? c.fechaAplicacion().plusDays(item.frecuenciaDias()) : null;
        AplicacionSanitaria value = new AplicacionSanitaria(UUID.randomUUID(), u.empresaId(), null,
                c.planItemId(), c.animalId(), null, null, c.dosis(), c.unidadDosis(), c.dosis(), c.dosis(),
                null, null, null, c.productoTexto(), null, null, null, null,
                item == null ? null : item.id(), item == null ? null : item.instruccionesVeterinario(), null,
                c.fechaAplicacion(), proxima, null, null, u.userId(), null, componerObservaciones(c),
                UUID.randomUUID().toString(), EstadoAplicacionSanitaria.APLICADO, 0,
                OrigenRegistroAplicacion.DECLARADA_PROVEEDOR);
        AplicacionSanitaria saved = repo.crearAplicacion(value, u.userId());
        if (item != null) programarAlerta(u, saved, item, animal);
        audit(u, "REGISTRAR_HISTORIAL_DECLARADO", saved.id());
        return saved;
    }

    private PlanSanitarioItem requireItemCompatible(UUID id, UUID empresa, TipoActividadSanitaria tipo) {
        PlanSanitarioItem item = planes.planes(empresa).stream()
                .flatMap(p -> planes.items(p.id(), empresa, false).stream())
                .filter(i -> i.id().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SANIDAD_PLAN_NOT_FOUND));
        if (!item.activo() || item.tipoActividad() != tipo) {
            throw new BusinessException(ErrorCode.SANIDAD_ITEM_TIPO_INCOMPATIBLE);
        }
        return item;
    }

    /**
     * AplicacionSanitaria no tiene columna propia para el tipo de actividad (a diferencia del
     * producto, que ya se guarda en productoAplicadoTexto); el tipo se deja en observaciones
     * para no perder lo declarado ni ampliar el esquema más allá de lo que pide esta fase.
     */
    private String componerObservaciones(RegistrarAplicacionDeclaradaCommand c) {
        List<String> partes = new ArrayList<>();
        partes.add("Historial declarado por el proveedor (" + c.tipoActividad().name() + ")");
        if (c.observaciones() != null && !c.observaciones().isBlank()) partes.add(c.observaciones());
        return String.join(" — ", partes);
    }

    private void programarAlerta(CurrentUser u, AplicacionSanitaria a, PlanSanitarioItem item, Animal animal) {
        MotorAlertas m = alertas.getIfAvailable();
        if (m == null || item.tipoActividad() != TipoActividadSanitaria.VACUNACION || a.proximaAplicacion() == null) return;
        ZoneId zona = ZoneId.of("America/La_Paz");
        for (UUID anterior : repo.aplicacionesPrevias(u.empresaId(), a.animalId(), item.id(), a.id())) {
            m.resolverPorOrigen(u.empresaId(), "APLICACION_SANITARIA", anterior);
        }
        Map<String, Object> datos = new HashMap<>();
        datos.put("animalCodigo", animal.codigo());
        if (animal.nombre() != null && !animal.nombre().isBlank()) datos.put("animalNombre", animal.nombre());
        datos.put("diasRestantes", item.diasAlerta());
        datos.put("fechaProximaAplicacion", a.proximaAplicacion().toString());
        datos.put("eventoReferencia", a.proximaAplicacion().toString());
        m.programar(new ProgramarAlertaCommand(u.empresaId(), a.animalId(), TipoAlerta.VACUNA_PROXIMA,
                a.proximaAplicacion().minusDays(item.diasAlerta()).atStartOfDay(zona).toInstant(),
                a.proximaAplicacion().atStartOfDay(zona).toInstant(),
                "APLICACION_SANITARIA", a.id(), datos));
    }

    private void audit(CurrentUser u, String action, UUID id) {
        events.publishEvent(new SanidadAuditEvent(u.empresaId(), u.userId(), action, "APLICACION_SANITARIA", id, Instant.now()));
    }
}
