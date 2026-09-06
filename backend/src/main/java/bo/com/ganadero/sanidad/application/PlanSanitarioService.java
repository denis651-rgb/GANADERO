package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PlanSanitarioService {
    private final SanidadRepository repo;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final EventoCalendarioSanitarioRepository eventos;

    public PlanSanitarioService(SanidadRepository repo, UserContext context, ApplicationEventPublisher events,
                               EventoCalendarioSanitarioRepository eventos) {
        this.eventos = eventos;
        this.repo = repo;
        this.context = context;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<Enfermedad> enfermedades(boolean inactivas) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        return repo.enfermedades(u.empresaId(), inactivas);
    }

    @Transactional
    public Enfermedad crearEnfermedad(CrearEnfermedadCommand c) {
        CurrentUser u = context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        UUID id = UUID.randomUUID();
        Enfermedad e = repo.crearEnfermedad(new Enfermedad(id, u.empresaId(), c.codigo().trim().toUpperCase(),
                c.nombre().trim(), c.descripcion(), c.esNotificable(), true, null, null));
        audit(u, "CREAR_ENFERMEDAD", "ENFERMEDAD", id);
        return e;
    }

    @Transactional
    public Enfermedad estadoEnfermedad(UUID id, boolean activo) {
        CurrentUser u = context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        Enfermedad e = repo.cambiarEstadoEnfermedad(id, u.empresaId(), activo);
        audit(u, activo ? "ACTIVAR_ENFERMEDAD" : "DESACTIVAR_ENFERMEDAD", "ENFERMEDAD", id);
        return e;
    }

    @Transactional(readOnly = true)
    public List<PlanSanitario> planes() {
        return repo.planes(context.requirePermission("SANIDAD_VER").empresaId());
    }

    @Transactional
    public PlanSanitario crearPlan(CrearPlanSanitarioCommand c) {
        CurrentUser u = context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        if (c.propiedadId() != null) context.requirePropertyAccess(u, c.propiedadId());
        if (c.fechaFin() != null && c.fechaFin().isBefore(c.fechaInicio())) {
            throw new BusinessException(ErrorCode.REPRODUCCION_FECHA_INVALIDA);
        }
        UUID id = UUID.randomUUID();
        PlanSanitario p = repo.crearPlan(new PlanSanitario(id, u.empresaId(), c.nombre(), c.descripcion(),
                c.fechaInicio(), c.fechaFin(), EstadoPlanSanitario.BORRADOR, null, null, 0, c.propiedadId()), u.userId());
        audit(u, "CREAR_PLAN_SANITARIO", "PLAN_SANITARIO", id);
        return p;
    }

    @Transactional
    public PlanSanitario cambiarEstado(UUID id, EstadoPlanSanitario nuevo, long version) {
        CurrentUser u = context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        PlanSanitario actual = requirePlan(id, u);
        if (!permitida(actual.estado(), nuevo)) throw new BusinessException(ErrorCode.SANIDAD_TRANSICION_INVALIDA);
        PlanSanitario p = repo.cambiarEstadoPlan(id, u.empresaId(), nuevo, version, u.userId());
        audit(u, "CAMBIAR_ESTADO_PLAN", "PLAN_SANITARIO", id);
        return p;
    }

    /**
     * Sección 18: ya no bloquea activar un plan aunque haya otro activo en el mismo alcance —
     * sólo informa cuáles se solapan (mismo tipo+modalidad de actividad vigente) para que el
     * usuario decida. No bloqueante a propósito.
     */
    @Transactional(readOnly = true)
    public List<PlanSanitario> conflictosActivacion(UUID id) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        PlanSanitario plan = requirePlan(id, u);
        List<TipoActividadSanitaria> tiposDelPlan = repo.items(id, u.empresaId(), false).stream()
                .map(PlanSanitarioItem::tipoActividad).distinct().toList();
        return repo.planesActivosEnAlcance(plan.propiedadId(), u.empresaId(), id).stream()
                .filter(otro -> repo.items(otro.id(), u.empresaId(), false).stream()
                        .anyMatch(item -> tiposDelPlan.contains(item.tipoActividad())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlanSanitarioItem> items(UUID plan, boolean inactivos) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        requirePlan(plan, u);
        return repo.items(plan, u.empresaId(), inactivos);
    }

    @Transactional(readOnly = true)
    public List<PlanSanitarioItem> versiones(UUID itemId) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        PlanSanitarioItem item = requireItem(itemId, u);
        return repo.versiones(item.identidadLogicaId(), u.empresaId());
    }

    @Transactional
    public PlanSanitarioItem crearItem(UUID planId, CrearPlanItemCommand c) {
        CurrentUser u = context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        requirePlan(planId, u);
        validar(c);
        UUID id = UUID.randomUUID();
        PlanSanitarioItem creado = repo.crearItem(construir(id, u.empresaId(), planId, c, id, 1, null,
                Instant.now(), null), u.userId());
        audit(u, "CREAR_ITEM_PLAN", "PLAN_SANITARIO_ITEM", id);
        return creado;
    }

    /**
     * Edita una actividad (sección 16-17). Si nunca fue usada, actualiza la misma fila. Si ya fue
     * usada y el cambio afecta algún campo versionable, cierra la vigencia de esta versión y crea
     * la siguiente (misma identidad lógica, número de versión incrementado) — nunca toca eventos,
     * jornadas o aplicaciones ya generados con la versión vieja.
     */
    @Transactional
    public PlanSanitarioItem actualizarItem(UUID planId, UUID itemId, CrearPlanItemCommand c, long version) {
        CurrentUser u = context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        requirePlan(planId, u);
        validar(c);
        PlanSanitarioItem actual = requireItem(itemId, u);
        if (actual.version() != version) throw new BusinessException(ErrorCode.VERSION_CONFLICT);

        boolean enUso = repo.itemEnUso(itemId);
        PlanSanitarioItem propuesto = construir(itemId, u.empresaId(), planId, c, actual.identidadLogicaId(),
                actual.numeroVersion(), actual.versionAnteriorId(), actual.vigenteDesde(), actual.vigenteHasta());

        if (!enUso || huella(actual).equals(huella(propuesto))) {
            PlanSanitarioItem guardado = repo.actualizarItem(propuesto, u.userId());
            audit(u, "ACTUALIZAR_ITEM_PLAN", "PLAN_SANITARIO_ITEM", itemId);
            return guardado;
        }

        if (c.motivoVersion() == null || c.motivoVersion().isBlank() || c.fechaVigencia() == null) {
            throw new BusinessException(ErrorCode.SANIDAD_VERSION_MOTIVO_REQUERIDO);
        }
        repo.cerrarVigenciaItem(itemId, c.fechaVigencia(), actual.version(), u.userId());
        UUID nuevoId = UUID.randomUUID();
        PlanSanitarioItem nuevaVersion = construir(nuevoId, u.empresaId(), planId, c, actual.identidadLogicaId(),
                actual.numeroVersion() + 1, itemId, c.fechaVigencia(), null);
        PlanSanitarioItem creado = repo.crearItem(nuevaVersion, u.userId());
        audit(u, "CREAR_VERSION_ACTIVIDAD", "PLAN_SANITARIO_ITEM", nuevoId);
        return creado;
    }

    @Transactional
    public PlanSanitarioItem estadoItem(UUID plan, UUID id, boolean activo, long version) {
        CurrentUser u = context.requirePermission("SANIDAD_PLAN_ADMINISTRAR");
        requirePlan(plan, u);
        return repo.cambiarEstadoItem(id, plan, u.empresaId(), activo, version, u.userId());
    }

    @Transactional(readOnly = true)
    public ProximaActividadSanitaria calcularProxima(UUID plan, UUID item, LocalDate aplicacion) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        PlanSanitario p = requirePlan(plan, u);
        if (p.estado() != EstadoPlanSanitario.ACTIVO) throw new BusinessException(ErrorCode.SANIDAD_TRANSICION_INVALIDA);
        PlanSanitarioItem i = repo.items(plan, u.empresaId(), false).stream().filter(x -> x.id().equals(item))
                .findFirst().orElseThrow(() -> new BusinessException(ErrorCode.SANIDAD_PLAN_NOT_FOUND));
        if (i.frecuenciaDias() == null) return new ProximaActividadSanitaria(null, null);
        LocalDate proxima = aplicacion.plusDays(i.frecuenciaDias());
        return new ProximaActividadSanitaria(proxima, proxima.minusDays(i.diasAlerta()));
    }

    private void validar(CrearPlanItemCommand c) {
        if (c.edadMinDias() != null && c.edadMaxDias() != null && c.edadMaxDias() < c.edadMinDias()) {
            throw new BusinessException(ErrorCode.SANIDAD_ITEM_EDAD_INVALIDA);
        }
        validarModalidad(c.modalidad(), c.modalidadConfig());
        validarViaLugar(c.viaAdministracionCodigo(), c.viaAdministracionDetalle(), c.lugarAplicacion(), c.lugarAplicacionDetalle());
        validarDosis(c.dosisTipoCalculo(), c.dosisCantidad(), c.dosisPesoReferenciaKg(), c.dosisMinima(), c.dosisMaxima());
        if (c.horaEjecucion() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La hora prevista de ejecución es obligatoria.");
        }
        if (c.horariosAviso() == null || c.horariosAviso().isEmpty() || c.horariosAviso().size() > 5
                || c.horariosAviso().stream().anyMatch(java.util.Objects::isNull)
                || c.horariosAviso().stream().distinct().count() != c.horariosAviso().size()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "Configure entre uno y cinco horarios de aviso diferentes.");
        }
    }

    private void validarModalidad(ModalidadActividad modalidad, ModalidadConfig config) {
        boolean ok = switch (modalidad) {
            case POR_EDAD -> config instanceof ModalidadConfig.PorEdadConfig cfg && cfg.edadObjetivoValor() > 0;
            case PERIODICA -> config instanceof ModalidadConfig.PeriodicaConfig cfg && cfg.frecuenciaValor() > 0;
            case FECHA_PROGRAMADA -> config instanceof ModalidadConfig.FechaProgramadaConfig cfg && cfg.fechaProgramada() != null;
            case POR_HALLAZGO -> config instanceof ModalidadConfig.PorHallazgoConfig cfg
                    && cfg.tiposHallazgo() != null && !cfg.tiposHallazgo().isEmpty();
            case MANUAL -> config instanceof ModalidadConfig.ManualConfig;
        };
        if (!ok) throw new BusinessException(ErrorCode.SANIDAD_MODALIDAD_CONFIG_INVALIDA);
    }

    private void validarViaLugar(ViaAdministracion via, String viaDetalle, LugarAplicacion lugar, String lugarDetalle) {
        if (via == null) return;
        boolean inyectable = via == ViaAdministracion.SUBCUTANEA || via == ViaAdministracion.INTRAMUSCULAR
                || via == ViaAdministracion.INTRAVENOSA;
        if (inyectable && (lugar == null || lugar == LugarAplicacion.NO_APLICA)) {
            throw new BusinessException(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE,
                    "Una vía inyectable requiere indicar el lugar anatómico.");
        }
        if (via == ViaAdministracion.ORAL && lugar != null && lugar != LugarAplicacion.BOCA && lugar != LugarAplicacion.NO_APLICA) {
            throw new BusinessException(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE,
                    "La vía oral sólo admite BOCA o NO_APLICA como lugar.");
        }
        if (via == ViaAdministracion.POUR_ON && lugar != null && lugar != LugarAplicacion.LINEA_DORSAL && lugar != LugarAplicacion.LOMO) {
            throw new BusinessException(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE,
                    "Pour-on sólo admite línea dorsal o lomo como lugar.");
        }
        if (via == ViaAdministracion.OTRA && (viaDetalle == null || viaDetalle.isBlank())) {
            throw new BusinessException(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE, "Indica el detalle de la vía.");
        }
        if (lugar == LugarAplicacion.OTRO && (lugarDetalle == null || lugarDetalle.isBlank())) {
            throw new BusinessException(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE, "Indica el detalle del lugar.");
        }
    }

    private void validarDosis(TipoCalculoDosis tipo, BigDecimal cantidad, BigDecimal pesoRef, BigDecimal min, BigDecimal max) {
        if (tipo == TipoCalculoDosis.POR_PESO && (cantidad == null || pesoRef == null || pesoRef.signum() <= 0)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "La dosis por peso requiere cantidad y peso de referencia mayor a cero.");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "La dosis mínima no puede ser mayor que la máxima.");
        }
    }

    private PlanSanitarioItem construir(UUID id, UUID empresa, UUID planId, CrearPlanItemCommand c,
                                       UUID identidadLogicaId, int numeroVersion, UUID versionAnteriorId,
                                       Instant vigenteDesde, Instant vigenteHasta) {
        UUID categoriaLegacy = c.categoriasAplicables() == null || c.categoriasAplicables().isEmpty()
                ? null : c.categoriasAplicables().get(0);
        Integer frecuenciaDiasLegacy = c.modalidadConfig() instanceof ModalidadConfig.PeriodicaConfig p
                ? diasDesde(p.frecuenciaValor(), p.frecuenciaUnidad()) : null;
        return new PlanSanitarioItem(id, empresa, planId, c.tipoActividad(), null, c.productoRecomendadoTexto(),
                categoriaLegacy, c.sexoAplicable(), c.edadMinDias(), c.edadMaxDias(), c.dosisCantidad(),
                c.dosisUnidad() == null ? null : c.dosisUnidad().name(), frecuenciaDiasLegacy, c.diasAlerta(),
                c.viaAdministracionCodigo() == null ? null : c.viaAdministracionCodigo().name(), c.obligatorio(),
                true, 0, c.origenRegulatorio(), c.especieAplicable(), c.permiteEdadDesconocida(),
                identidadLogicaId, numeroVersion, versionAnteriorId, vigenteDesde, vigenteHasta, c.motivoVersion(),
                c.codigoInterno(), c.nombre(), c.descripcion(), c.principioActivo(), c.instruccionesVeterinario(),
                c.observaciones(), c.dosisCantidad(), c.dosisUnidad(), c.dosisUnidadDetalle(), c.dosisTipoCalculo(),
                c.dosisPesoReferenciaKg(), c.dosisMinima(), c.dosisMaxima(), c.viaAdministracionCodigo(),
                c.viaAdministracionDetalle(), c.lugarAplicacion(), c.lugarAplicacionDetalle(),
                c.categoriasAplicables() == null ? List.of() : c.categoriasAplicables(), c.edadUnidad(),
                c.modalidad(), c.modalidadConfig(), false, c.horaEjecucion(), c.horariosAviso());
    }

    private Integer diasDesde(int valor, UnidadFrecuencia unidad) {
        return switch (unidad) {
            case DIAS -> valor;
            case SEMANAS -> valor * 7;
            case MESES -> valor * 30;
            case ANIOS -> valor * 365;
        };
    }

    /** Huella de los campos "versionables" (sección 17): si dos versiones coinciden en esto, el cambio es puramente administrativo. */
    private record Huella(String nombre, TipoActividadSanitaria tipo, ModalidadActividad modalidad,
                          ModalidadConfig modalidadConfig, String producto, String principioActivo,
                          BigDecimal dosisCantidad, UnidadDosis dosisUnidad, TipoCalculoDosis dosisTipoCalculo,
                          BigDecimal dosisMinima, BigDecimal dosisMaxima, ViaAdministracion via, LugarAplicacion lugar,
                          Integer edadMin, Integer edadMax, bo.com.ganadero.animales.domain.SexoAnimal sexo,
                          List<UUID> categorias, String instrucciones, int diasAlerta,
                          java.time.LocalTime horaEjecucion, List<java.time.LocalTime> horariosAviso) {
    }

    private Huella huella(PlanSanitarioItem i) {
        return new Huella(i.nombre(), i.tipoActividad(), i.modalidad(), i.modalidadConfig(),
                i.productoRecomendadoTexto(), i.principioActivo(), i.dosisCantidad(), i.dosisUnidad(),
                i.dosisTipoCalculo(), i.dosisMinima(), i.dosisMaxima(), i.viaAdministracionCodigo(), i.lugarAplicacion(),
                i.edadMinDias(), i.edadMaxDias(), i.sexoAplicable(), i.categoriasAplicables(),
                i.instruccionesVeterinario(), i.diasAlerta(), i.horaEjecucion(), i.horariosAviso());
    }

    @Transactional(readOnly = true)
    public List<EventoCalendarioSanitario> calendario(EstadoEventoCalendario estado, UUID animalId) {
        CurrentUser u = context.requirePermission("SANIDAD_VER");
        return eventos.listar(u.empresaId(), estado, animalId, null, null, 500);
    }

    private PlanSanitario requirePlan(UUID id, CurrentUser u) {
        return repo.plan(id, u.empresaId()).orElseThrow(() -> new BusinessException(ErrorCode.SANIDAD_PLAN_NOT_FOUND));
    }

    private PlanSanitarioItem requireItem(UUID id, CurrentUser u) {
        return repo.item(id, u.empresaId()).orElseThrow(() -> new BusinessException(ErrorCode.SANIDAD_ITEM_NOT_FOUND));
    }

    private boolean permitida(EstadoPlanSanitario a, EstadoPlanSanitario n) {
        return switch (a) {
            case BORRADOR -> n == EstadoPlanSanitario.ACTIVO || n == EstadoPlanSanitario.ANULADO;
            case ACTIVO -> n == EstadoPlanSanitario.FINALIZADO || n == EstadoPlanSanitario.ANULADO;
            case FINALIZADO, ANULADO -> false;
        };
    }

    private void audit(CurrentUser u, String accion, String entidad, UUID id) {
        events.publishEvent(new SanidadAuditEvent(u.empresaId(), u.userId(), accion, entidad, id, Instant.now()));
    }
}
