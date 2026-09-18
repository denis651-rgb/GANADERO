package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class JornadaSanitariaService {

    private final JornadaSanitariaRepository repo;
    private final SanidadRepository planes;
    private final AnimalRepository animales;
    private final UserContext context;
    private final DosisCalculadaService dosisCalculada;
    private final EventoCalendarioSanitarioRepository eventos;
    private final ObjectProvider<MotorAlertas> alertas;
    private final TimelineEventPublisher timeline;
    private final ApplicationEventPublisher events;

    public JornadaSanitariaService(JornadaSanitariaRepository repo, SanidadRepository planes,
                                   AnimalRepository animales, UserContext context,
                                   DosisCalculadaService dosisCalculada, EventoCalendarioSanitarioRepository eventos,
                                   ObjectProvider<MotorAlertas> alertas, TimelineEventPublisher timeline,
                                   ApplicationEventPublisher events) {
        this.repo = repo;
        this.planes = planes;
        this.animales = animales;
        this.context = context;
        this.dosisCalculada = dosisCalculada;
        this.eventos = eventos;
        this.alertas = alertas;
        this.timeline = timeline;
        this.events = events;
    }

    @Transactional
    public JornadaSanitaria crear(CrearJornadaCommand c) {
        CurrentUser u = context.requirePermission("SANIDAD_JORNADA_CREAR");
        context.requirePropertyAccess(u, c.propiedadId());
        ReglasSanitarias.exigir(c.fechaInicio() != null, "La fecha de inicio es obligatoria.");
        UUID id = UUID.randomUUID();
        return repo.crear(new JornadaSanitaria(id, u.empresaId(), c.tipoJornada(), c.fechaInicio(), null,
                c.propiedadId(), c.potreroId(), c.loteGanaderoId(), c.responsableId(), c.veterinarioId(),
                EstadoJornada.BORRADOR, c.observaciones(), null, 0), u.userId());
    }

    @Transactional
    public JornadaSanitaria actualizar(UUID id, ActualizarJornadaCommand c) {
        CurrentUser u = context.requirePermission("SANIDAD_JORNADA_CREAR");
        JornadaSanitaria actual = require(id, u);
        ReglasSanitarias.exigir(actual.estado() == EstadoJornada.BORRADOR, "Solo puede editar una jornada en borrador.");
        context.requirePropertyAccess(u, c.propiedadId());
        ReglasSanitarias.exigir(c.fechaInicio() != null, "La fecha de inicio es obligatoria.");
        JornadaSanitaria cambios = new JornadaSanitaria(actual.id(), u.empresaId(), c.tipoJornada(), c.fechaInicio(),
                null, c.propiedadId(), c.potreroId(), c.loteGanaderoId(), c.responsableId(), c.veterinarioId(),
                EstadoJornada.BORRADOR, c.observaciones(), null, c.version());
        JornadaSanitaria guardada = repo.actualizar(cambios, u.userId());
        repo.reemplazarSeleccion(id, u.empresaId(), List.of());
        audit(u, "ACTUALIZAR_JORNADA_SANITARIA", "JORNADA_SANITARIA", id);
        return guardada;
    }

    @Transactional
    public JornadaSanitaria anular(UUID id, long version, String motivo) {
        CurrentUser u = context.requirePermission("SANIDAD_JORNADA_CREAR");
        JornadaSanitaria actual = require(id, u);
        ReglasSanitarias.exigir(actual.estado() == EstadoJornada.BORRADOR, "Solo puede anular una jornada en borrador.");
        ReglasSanitarias.exigir(motivo != null && !motivo.isBlank(), "El motivo de anulación es obligatorio.");
        JornadaSanitaria anulada = repo.anular(id, u.empresaId(), version, motivo, u.userId());
        repo.reemplazarSeleccion(id, u.empresaId(), List.of());
        audit(u, "ANULAR_JORNADA_SANITARIA", "JORNADA_SANITARIA", id);
        return anulada;
    }

    @Transactional(readOnly = true)
    public List<JornadaSanitaria> listar() {
        return repo.listar(context.requirePermission("SANIDAD_VER").empresaId());
    }

    @Transactional(readOnly = true)
    public List<AplicacionSanitaria> aplicaciones(UUID id) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        require(id, u);
        return repo.aplicaciones(id, u.empresaId());
    }

    @Transactional(readOnly = true)
    public List<Animal> elegibles(UUID propiedad, UUID lote, UUID categoria, SexoAnimal sexo) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        context.requirePropertyAccess(u, propiedad);
        return animales.findEligible(u.empresaId(), propiedad, lote, categoria, sexo);
    }

    @Transactional(readOnly = true)
    public ResultadoElegibilidad elegibilidad(UUID id, UUID planItemId, java.time.LocalDate fechaAplicacion) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        JornadaSanitaria j = require(id, u);
        PlanSanitarioItem item = requireItemCompatible(planItemId, u.empresaId(), j);
        List<AnimalElegibilidad> resultado = animales.findEligible(u.empresaId(), j.propiedadId(), j.loteGanaderoId(), null, null)
                .stream().filter(a -> j.potreroId() == null || j.potreroId().equals(a.potreroActualId()))
                .map(a -> evaluar(a, j, item, fechaAplicacion)).toList();
        return ResultadoElegibilidad.of(resultado);
    }

    @Transactional
    public List<UUID> seleccionar(UUID id, UUID planItemId, java.time.LocalDate fechaAplicacion, List<UUID> ids) {
        CurrentUser u = context.requirePermission("SANIDAD_JORNADA_CREAR");
        JornadaSanitaria j = require(id, u);
        ReglasSanitarias.exigir(j.estado() == EstadoJornada.BORRADOR, "Solo puede cambiar la selección de una jornada en borrador.");
        PlanSanitarioItem item = requireItemCompatible(planItemId, u.empresaId(), j);
        for (UUID animalId : ids) {
            Animal a = animal(u, animalId);
            AnimalElegibilidad resultado = evaluar(a, j, item, fechaAplicacion);
            if (!resultado.elegible()) {
                throw new BusinessException(ErrorCode.SANIDAD_ANIMAL_NO_ELEGIBLE,
                        a.codigo() + ": " + String.join(" ", resultado.motivos()));
            }
        }
        repo.reemplazarSeleccion(id, u.empresaId(), ids);
        return repo.seleccion(id, u.empresaId());
    }

    @Transactional
    public ConfirmacionJornadaResult confirmar(UUID id, ConfirmarJornadaCommand c) {
        CurrentUser u = context.requirePermission("SANIDAD_JORNADA_CONFIRMAR");
        Optional<JornadaSanitaria> previa = repo.buscarPorOperacion(c.operationId(), u.empresaId());
        if (previa.isPresent() && previa.get().estado() == EstadoJornada.CONFIRMADA) {
            List<AplicacionSanitaria> ya = repo.aplicaciones(previa.get().id(), u.empresaId());
            return new ConfirmacionJornadaResult(previa.get(), ya, ya.size());
        }
        JornadaSanitaria j = require(id, u);
        List<UUID> seleccion = repo.seleccion(id, u.empresaId());
        if (seleccion.isEmpty()) throw new BusinessException(ErrorCode.SANIDAD_JORNADA_SIN_ANIMALES);
        PlanSanitarioItem item = requireItemCompatible(c.planItemId(), u.empresaId(), j);
        repo.iniciarConfirmacion(id, u.empresaId(), c.version(), c.operationId(), u.userId());

        String productoAplicado = c.productoAplicadoTexto() != null ? c.productoAplicadoTexto() : item.productoRecomendadoTexto();
        boolean productoCambio = item.productoRecomendadoTexto() != null
                && !item.productoRecomendadoTexto().equals(productoAplicado);
        if (productoCambio && (c.motivoCambioProducto() == null || c.motivoCambioProducto().isBlank())) {
            throw new BusinessException(ErrorCode.SANIDAD_PRODUCTO_CAMBIO_MOTIVO_REQUERIDO);
        }
        LugarAplicacion lugar = c.lugarAplicacion() != null ? c.lugarAplicacion() : item.lugarAplicacion();

        List<AplicacionSanitaria> out = new ArrayList<>();
        Set<UUID> ocurrenciasAplicadas = new HashSet<>();
        for (UUID aid : seleccion) {
            Animal a = animal(u, aid);
            AnimalElegibilidad elegibilidad = evaluar(a, j, item, c.fechaAplicacion());
            if (!elegibilidad.elegible()) {
                throw new BusinessException(ErrorCode.SANIDAD_ANIMAL_NO_ELEGIBLE,
                        a.codigo() + ": " + String.join(" ", elegibilidad.motivos()));
            }
            ReglasSanitarias.intervaloMinimoEntreAplicaciones(repo, u.empresaId(), a.id(), item, item.productoId(), c.fechaAplicacion());

            CalculoDosis calculo = dosisCalculada.calcular(item, aid, u.empresaId());
            BigDecimal dosisAplicada = c.dosisAplicada() != null ? c.dosisAplicada() : calculo.dosisCalculada();
            boolean dosisAjustada = calculo.dosisCalculada() != null && dosisAplicada != null
                    && calculo.dosisCalculada().compareTo(dosisAplicada) != 0;
            if (dosisAjustada && (c.motivoAjusteDosis() == null || c.motivoAjusteDosis().isBlank())) {
                throw new BusinessException(ErrorCode.SANIDAD_DOSIS_AJUSTE_MOTIVO_REQUERIDO);
            }

            java.time.LocalDate fecha = c.fechaAplicacion();
            java.time.LocalDate prox = item.frecuenciaDias() != null ? fecha.plusDays(item.frecuenciaDias()) : null;
            java.time.LocalDate rc = c.retiroCarneDias() == null || c.retiroCarneDias() == 0 ? null : fecha.plusDays(c.retiroCarneDias());
            java.time.LocalDate rl = c.retiroLecheDias() == null || c.retiroLecheDias() == 0 ? null : fecha.plusDays(c.retiroLecheDias());
            String key = c.operationId() + ":" + aid;

            Optional<EventoCalendarioSanitario> eventoACerrar = ReglasSanitarias.eventoQueCierraLaAplicacion(
                    eventos.cerrablesPorAplicacion(item.id(), aid), fecha);
            eventoACerrar.map(EventoCalendarioSanitario::ocurrenciaId).ifPresent(ocurrenciasAplicadas::add);

            AplicacionSanitaria ap = repo.crearAplicacion(new AplicacionSanitaria(UUID.randomUUID(), u.empresaId(),
                    id, c.planItemId(), aid, null, null, dosisAplicada, c.unidadDosis(), calculo.dosisCalculada(),
                    dosisAplicada, calculo.pesoUsadoKg(), calculo.pesoTipo(), calculo.pesoFecha(), productoAplicado,
                    productoCambio ? c.motivoCambioProducto() : null, dosisAjustada ? c.motivoAjusteDosis() : null,
                    c.viaAdministracion(), lugar, item.id(), item.instruccionesVeterinario(),
                    eventoACerrar.map(EventoCalendarioSanitario::id).orElse(null), fecha, prox, rc, rl, u.userId(),
                    c.resultado(), c.observaciones(), key, EstadoAplicacionSanitaria.APLICADO, 0,
                    OrigenRegistroAplicacion.APLICADA_FINCA), u.userId());
            out.add(ap);
            eventoACerrar.ifPresent(evt -> eventos.marcarEstado(evt.id(), EstadoEventoCalendario.REALIZADO, id, u.userId()));
            publicar(u, ap, j);
            programar(u, ap, item);
            audit(u, "APLICAR_SANIDAD", "APLICACION_SANITARIA", ap.id());
        }
        JornadaSanitaria done = repo.confirmar(id, u.empresaId(), u.userId());
        resolverOcurrenciasAplicadas(u, ocurrenciasAplicadas);
        audit(u, "CONFIRMAR_JORNADA_SANITARIA", "JORNADA_SANITARIA", id);
        return new ConfirmacionJornadaResult(done, out, out.size());
    }

    /**
     * Cierra la alerta grupal de cada ocurrencia que quedó completamente aplicada. Si todavía
     * queda algún evento sin cerrar en la ocurrencia —pendiente o vencido, porque se aplicó sólo
     * una parte del grupo—, la alerta se mantiene para no perder el seguimiento de los animales
     * restantes.
     */
    private void resolverOcurrenciasAplicadas(CurrentUser u, Set<UUID> ocurrenciasAplicadas) {
        MotorAlertas m = alertas.getIfAvailable();
        if (m == null) return;
        for (UUID ocurrenciaId : ocurrenciasAplicadas) {
            if (!eventos.tieneSinCerrar(ocurrenciaId)) {
                m.resolverPorOrigen(u.empresaId(), "EVENTO_CALENDARIO_SANITARIO", ocurrenciaId);
            }
        }
    }

    private PlanSanitarioItem buscarItem(UUID id, UUID e) {
        return planes.planes(e).stream().flatMap(p -> planes.items(p.id(), e, false).stream())
                .filter(i -> i.id().equals(id)).findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.SANIDAD_PLAN_NOT_FOUND));
    }

    private PlanSanitarioItem requireItemCompatible(UUID id, UUID empresa, JornadaSanitaria jornada) {
        PlanSanitarioItem item = buscarItem(id, empresa);
        if (!item.activo() || item.tipoActividad() != jornada.tipoJornada()) {
            throw new BusinessException(ErrorCode.SANIDAD_ITEM_JORNADA_INCOMPATIBLE);
        }
        return item;
    }

    private AnimalElegibilidad evaluar(Animal a, JornadaSanitaria j, PlanSanitarioItem i, java.time.LocalDate fecha) {
        List<String> motivos = new ArrayList<>();
        try {
            ReglasSanitarias.fechaAnimal(a, fecha, false);
            ReglasSanitarias.exigir(!fecha.isBefore(j.fechaInicio()), "La aplicación no puede ser anterior al inicio de la jornada.");
        } catch (BusinessException ex) {
            motivos.add(ex.getMessage());
        }
        if (a.estado() != EstadoAnimal.ACTIVO) motivos.add("El animal no está activo.");
        if (!a.propiedadActualId().equals(j.propiedadId())) motivos.add("El animal ya no pertenece a la propiedad de la jornada.");
        if (j.potreroId() != null && !j.potreroId().equals(a.potreroActualId())) motivos.add("El animal no pertenece al potrero de la jornada.");
        if (j.loteGanaderoId() != null && !j.loteGanaderoId().equals(a.loteActualId())) motivos.add("El animal no pertenece al lote de la jornada.");
        if (i.sexoAplicable() != null && i.sexoAplicable() != a.sexo()) motivos.add("El sexo del animal no coincide con la actividad.");
        if (!i.categoriasAplicables().isEmpty() && !i.categoriasAplicables().contains(a.categoriaActualId())) {
            motivos.add("La categoría del animal no coincide con la actividad.");
        }
        Long edad = null;
        if (a.fechaNacimiento() != null) edad = ChronoUnit.DAYS.between(a.fechaNacimiento(), fecha);
        motivos.addAll(ReglasSanitarias.motivosEdad(a.fechaNacimiento(), fecha, i.edadMinDias(), i.edadMaxDias(),
                i.permiteEdadDesconocida()));
        return new AnimalElegibilidad(a.id(), a.codigo(), a.nombre(), a.sexo(), a.estado(), edad,
                a.fechaNacimientoEstimada(), motivos.isEmpty(), List.copyOf(motivos));
    }

    private void publicar(CurrentUser u, AplicacionSanitaria a, JornadaSanitaria j) {
        TipoEventoAnimal t = j.tipoJornada() == TipoActividadSanitaria.VACUNACION
                ? TipoEventoAnimal.VACUNACION_APLICADA : TipoEventoAnimal.TRATAMIENTO_APLICADO;
        timeline.publish(new RegistrarEventoTimeline(u.empresaId(), a.animalId(), t, null, j.tipoJornada().name(),
                null, a.id(), Map.of("jornadaId", j.id()), u.userId(), Instant.now(), a.idempotencyKey()));
    }

    /**
     * Las alertas "próxima/vencida" de una actividad del plan ya no se crean por animal al
     * confirmar: {@link CalendarioSanitarioService} las genera una sola vez por ocurrencia
     * (actividad + fecha + ubicación). Acá sólo se resuelven los avisos previos de la aplicación
     * anterior y se programan los retiros, que sí son individuales por animal.
     */
    private void programar(CurrentUser u, AplicacionSanitaria a, PlanSanitarioItem i) {
        MotorAlertas m = alertas.getIfAvailable();
        if (m == null) return;
        for (UUID anterior : repo.aplicacionesPrevias(u.empresaId(), a.animalId(), i.id(), a.id())) {
            m.resolverPorOrigen(u.empresaId(), "APLICACION_SANITARIA", anterior);
        }
        if (a.retiroCarneHasta() != null) {
            m.programar(ProgramarAlertaCommand.alDia(u.empresaId(), a.animalId(), TipoAlerta.RETIRO_CARNE_VIGENTE,
                    a.retiroCarneHasta(), null, "RETIRO_CARNE", a.id(),
                    Map.of("tipoRetiro", "CARNE", "hasta", a.retiroCarneHasta().toString())));
        }
        if (a.retiroLecheHasta() != null) {
            m.programar(ProgramarAlertaCommand.alDia(u.empresaId(), a.animalId(), TipoAlerta.RETIRO_LECHE_VIGENTE,
                    a.retiroLecheHasta(), null, "RETIRO_LECHE", a.id(),
                    Map.of("tipoRetiro", "LECHE", "hasta", a.retiroLecheHasta().toString())));
        }
    }

    private JornadaSanitaria require(UUID id, CurrentUser u) {
        JornadaSanitaria j = repo.buscar(id, u.empresaId()).orElseThrow(() -> new BusinessException(ErrorCode.SANIDAD_JORNADA_NOT_FOUND));
        context.requirePropertyAccess(u, j.propiedadId());
        return j;
    }

    private Animal animal(CurrentUser u, UUID id) {
        return animales.findById(id, u.empresaId()).orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    }

    private void audit(CurrentUser u, String accion, String entidad, UUID id) {
        events.publishEvent(new SanidadAuditEvent(u.empresaId(), u.userId(), accion, entidad, id, Instant.now()));
    }
}
