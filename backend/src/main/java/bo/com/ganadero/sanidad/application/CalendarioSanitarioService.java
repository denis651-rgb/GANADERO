package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.sanidad.domain.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Calendario sanitario genérico (secciones 19-20): a diferencia de
 * {@link ProyectarCalendarioSanitarioService} (acotado a VACUNACION por diseño de la Fase 1),
 * este servicio funciona para cualquier {@link TipoActividadSanitaria} y cualquier
 * {@link ModalidadActividad} orientada a fecha (POR_EDAD/PERIODICA/FECHA_PROGRAMADA).
 * POR_HALLAZGO no se genera aquí — se dispara desde {@link ClinicaService} cuando ocurre el
 * hallazgo real; MANUAL nunca genera nada automáticamente.
 *
 * <p><strong>Fase 1 de la integración con Google Calendar</strong> (ver plan de implementación):
 * a diferencia de la versión anterior, este servicio (a) preserva la hora configurada de cada
 * actividad en vez de truncar todo a las 00:00, (b) proyecta con un horizonte configurable de
 * meses hacia adelante (no sólo cuando la ventana de anticipación ya abrió) marcando los eventos
 * lejanos como {@link EstadoEventoCalendario#PROYECTADO} hasta que la ventana realmente se
 * acerca, (c) respeta el alcance por propiedad del plan (sección 18) al buscar animales candidatos,
 * y (d) agrupa los eventos de varios animales que comparten actividad+fecha+ubicación en una
 * {@link OcurrenciaCalendarioSanitario}, previendo la futura sincronización externa.</p>
 *
 * <p>Simplificaciones documentadas: para PERIODICA, cuando la referencia es
 * {@code FECHA_CONFIGURADA} o {@code ULTIMA_APLICACION} sin ningún antecedente todavía, se usa
 * la fecha de vigencia de la actividad como semilla (no existe un campo separado de "fecha
 * configurada" en este modelo). La repetición de FECHA_PROGRAMADA ({@code reglaRepeticion}) no
 * está implementada — sólo se genera la fecha única configurada.</p>
 */
@Service
public class CalendarioSanitarioService {
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final UUID EMPRESA_LOCAL = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final int HORIZONTE_DEFAULT_MESES = 12;
    /** Tope de seguridad por actividad+animal en una sola corrida, para que una frecuencia muy corta no dispare miles de filas. */
    private static final int MAX_CICLOS_PERIODICA = 36;

    private final SanidadRepository planes;
    private final JdbcClient jdbc;
    private final EventoCalendarioSanitarioRepository eventos;
    private final OcurrenciaCalendarioSanitarioRepository ocurrencias;
    private final ObjectProvider<MotorAlertas> alertas;

    public CalendarioSanitarioService(SanidadRepository planes, JdbcClient jdbc,
                                      EventoCalendarioSanitarioRepository eventos,
                                      OcurrenciaCalendarioSanitarioRepository ocurrencias,
                                      ObjectProvider<MotorAlertas> alertas) {
        this.planes = planes;
        this.jdbc = jdbc;
        this.eventos = eventos;
        this.ocurrencias = ocurrencias;
        this.alertas = alertas;
    }

    @Transactional
    public int procesar() {
        marcarVencidos();
        promoverProgramados();
        int generados = 0;
        LocalDate hoy = LocalDate.now(ZONA);
        int horizonteMeses = horizonteProyeccionMeses();
        for (PlanSanitario plan : planes.planes(null)) {
            if (plan.estado() != EstadoPlanSanitario.ACTIVO) continue;
            for (PlanSanitarioItem item : planes.items(plan.id(), null, false)) {
                if (!item.activo() || item.vigenteHasta() != null) continue;
                generados += switch (item.modalidad()) {
                    case POR_EDAD -> procesarPorEdad(item, plan, hoy, horizonteMeses);
                    case PERIODICA -> procesarPeriodica(item, plan, hoy, horizonteMeses);
                    case FECHA_PROGRAMADA -> procesarFechaProgramada(item, plan, hoy, horizonteMeses);
                    case POR_HALLAZGO, MANUAL -> 0;
                };
            }
        }
        return generados;
    }

    private void marcarVencidos() {
        jdbc.sql("""
                update evento_calendario_sanitario set estado='VENCIDO',
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), version=version+1
                where estado in ('PROYECTADO','PROGRAMADO') and fecha_prevista < :ahora
                """).param("ahora", Instant.now().toString()).update();
    }

    /** Un evento generado con meses de anticipación nace PROYECTADO; recién pasa a PROGRAMADO cuando se entra a su ventana real. */
    private void promoverProgramados() {
        jdbc.sql("""
                update evento_calendario_sanitario set estado='PROGRAMADO',
                    updated_at=strftime('%Y-%m-%dT%H:%M:%fZ','now'), version=version+1
                where estado='PROYECTADO' and ventana_desde <= :ahora
                """).param("ahora", Instant.now().toString()).update();
    }

    private int horizonteProyeccionMeses() {
        return jdbc.sql("select horizonte_proyeccion_meses from configuracion_sanitaria limit 1")
                .query(Integer.class).optional().orElse(HORIZONTE_DEFAULT_MESES);
    }

    private int procesarPorEdad(PlanSanitarioItem item, PlanSanitario plan, LocalDate hoy, int horizonteMeses) {
        if (!(item.modalidadConfig() instanceof ModalidadConfig.PorEdadConfig cfg)) return 0;
        int generados = 0;
        Map<UUID, OcurrenciaProgramada> programadas = new LinkedHashMap<>();
        LocalDate limite = hoy.plusMonths(horizonteMeses);
        Set<UUID> yaLaRecibieron = cfg.unaVezEnLaVida() ? animalesQueYaRecibieron(item) : Set.of();
        for (CandidatoAnimal a : candidatos(item, plan.propiedadId())) {
            if (a.fechaNacimiento() == null) continue; // EXCLUIR e INCLUIR_MANUAL ambos evitan la generación automática
            if (a.fechaNacimientoEstimada() && cfg.politicaEdadEstimada() == PoliticaEdadEstimada.EXCLUIR) continue;
            if (yaLaRecibieron.contains(a.animalId())) continue; // «una sola vez en la vida»: no se vuelve a programar
            int edadObjetivoDias = aDias(cfg.edadObjetivoValor(), cfg.edadUnidad());
            LocalDate fechaObjetivo = a.fechaNacimiento().plusDays(edadObjetivoDias);
            if (fechaObjetivo.isAfter(limite)) continue; // fuera del horizonte de proyección
            LocalDate ventanaDesde = fechaObjetivo.minusDays(cfg.ventanaAnticipadaDias());
            LocalDate ventanaHasta = fechaObjetivo.plusDays(cfg.ventanaPosteriorDias());
            if (ventanaCerradaAntesDeLaVigencia(item, ventanaHasta)) continue;
            if (!edadElegible(item, a, fechaObjetivo)) continue;
            String ciclo = "EDAD:" + edadObjetivoDias;
            Instant fechaPrevista = fechaObjetivo.atTime(item.horaEjecucion()).atZone(ZONA).toInstant();
            acumular(programadas, crear(item, a, ciclo, fechaPrevista, ventanaDesde.atStartOfDay(ZONA).toInstant(),
                    ventanaHasta.atStartOfDay(ZONA).toInstant(), ventanaDesde, hoy, ModalidadActividad.POR_EDAD));
            generados++;
        }
        programarAlertas(item, programadas.values());
        return generados;
    }

    /**
     * Animales que ya recibieron esta actividad, en cualquiera de sus versiones (misma identidad
     * lógica): editar una actividad ya usada crea otra fila con otro id, y sin este cruce el
     * calendario volvería a programarla para todo el hato. Cuenta lo aplicado en finca y lo
     * declarado por el proveedor (ambos son {@code aplicacion_sanitaria} vinculada al item); una
     * aplicación anulada no cuenta porque el animal, en realidad, no la recibió.
     */
    private Set<UUID> animalesQueYaRecibieron(PlanSanitarioItem item) {
        return new HashSet<>(jdbc.sql("""
                select distinct a.animal_id from aplicacion_sanitaria a
                join plan_sanitario_item i on i.id = a.plan_item_id
                where i.identidad_logica_id = :identidad and a.estado = 'APLICADO'
                """).param("identidad", item.identidadLogicaId().toString())
                .query((r, n) -> UUID.fromString(r.getString("animal_id"))).list());
    }

    /**
     * Sin eventos retroactivos: si la ventana de una fecha se cerró antes de que la actividad
     * existiera en el plan, nada se debía bajo ese plan y generarla sólo llenaría el calendario de
     * vencidos (p. ej. «a los 7 meses» agregada a un hato adulto). Si la ventana se cerró estando
     * la actividad vigente sí se genera, y queda VENCIDO como un incumplimiento real.
     */
    private boolean ventanaCerradaAntesDeLaVigencia(PlanSanitarioItem item, LocalDate ventanaHasta) {
        return ventanaHasta.isBefore(item.vigenteDesde().atZone(ZONA).toLocalDate());
    }

    /** Aplica «Edad de los animales elegibles» a la edad que tendría el animal en la fecha del evento. */
    private boolean edadElegible(PlanSanitarioItem item, CandidatoAnimal a, LocalDate fechaEvento) {
        return ReglasSanitarias.motivosEdad(a.fechaNacimiento(), fechaEvento, item.edadMinDias(), item.edadMaxDias(),
                item.permiteEdadDesconocida()).isEmpty();
    }

    /**
     * Genera la serie de fechas de cada animal y la concilia con lo que ya estaba agendado. Con
     * «Última aplicación» la referencia se mueve cada vez que se registra una aplicación: sin
     * conciliar, la serie anterior seguiría viva y el animal quedaría con dos series mezcladas
     * (dos fechas por ciclo, dos avisos, y fantasmas que terminan vencidos). Por eso, en cada
     * corrida se cancelan los eventos pendientes que ya no pertenecen a la serie vigente y se
     * restauran los cancelados que vuelven a pertenecerle.
     */
    private int procesarPeriodica(PlanSanitarioItem item, PlanSanitario plan, LocalDate hoy, int horizonteMeses) {
        if (!(item.modalidadConfig() instanceof ModalidadConfig.PeriodicaConfig cfg)) return 0;
        int generados = 0;
        Map<UUID, OcurrenciaProgramada> programadas = new LinkedHashMap<>();
        int frecuenciaDias = aDiasFrecuencia(cfg.frecuenciaValor(), cfg.frecuenciaUnidad());
        LocalDate limite = hoy.plusMonths(horizonteMeses);
        // frecuenciaDias<=0 no debería ocurrir en datos validados por PlanSanitarioService, pero si
        // ocurre (dato legado) sólo se genera un ciclo — con frecuencia real, se proyectan todos los
        // ciclos que entren en el horizonte (tope MAX_CICLOS_PERIODICA por seguridad).
        int maxCiclos = frecuenciaDias <= 0 ? 1 : MAX_CICLOS_PERIODICA;
        Map<UUID, LocalDate> ultimasAplicaciones = cfg.referenciaCalculo() == ReferenciaCalculoPeriodica.ULTIMA_APLICACION
                ? ultimasAplicaciones(item) : Map.of();
        Map<UUID, List<EventoCalendarioSanitario>> existentes = eventos.pendientesOCanceladosFuturosDeActividad(item.id())
                .stream().collect(Collectors.groupingBy(EventoCalendarioSanitario::animalId));
        Set<UUID> ocurrenciasCanceladas = new HashSet<>();
        for (CandidatoAnimal a : candidatos(item, plan.propiedadId())) {
            LocalDate referencia = referenciaPeriodica(cfg.referenciaCalculo(), item, plan, a, ultimasAplicaciones);
            List<CicloPeriodico> ciclos = new ArrayList<>();
            if (referencia != null) {
                for (int ciclo = 1; ciclo <= maxCiclos; ciclo++) {
                    LocalDate proxima = referencia.plusDays((long) frecuenciaDias * ciclo);
                    if (proxima.isAfter(limite)) break; // ciclos futuros son estrictamente crecientes
                    LocalDate ventanaDesde = proxima.minusDays(cfg.toleranciaAnticipadaDias());
                    LocalDate ventanaHasta = proxima.plusDays(cfg.toleranciaPosteriorDias());
                    if (ventanaCerradaAntesDeLaVigencia(item, ventanaHasta)) continue;
                    if (!edadElegible(item, a, proxima)) continue;
                    ciclos.add(new CicloPeriodico("PERIODO:" + proxima, proxima, ventanaDesde, ventanaHasta));
                }
            }
            conciliarSerie(existentes.getOrDefault(a.animalId(), List.of()), ciclos, ocurrenciasCanceladas);
            for (CicloPeriodico c : ciclos) {
                Instant fechaPrevista = c.fecha().atTime(item.horaEjecucion()).atZone(ZONA).toInstant();
                acumular(programadas, crear(item, a, c.clave(), fechaPrevista, c.ventanaDesde().atStartOfDay(ZONA).toInstant(),
                        c.ventanaHasta().atStartOfDay(ZONA).toInstant(), c.ventanaDesde(), hoy, ModalidadActividad.PERIODICA));
                generados++;
            }
        }
        resolverAlertasSinPendientes(item, ocurrenciasCanceladas);
        programarAlertas(item, programadas.values());
        return generados;
    }

    /**
     * Deja los eventos ya agendados de un animal en línea con la serie vigente: cancela los
     * pendientes cuya fecha ya no pertenece a ella y restaura los cancelados que vuelven a
     * pertenecerle (si no, la clave única del calendario impediría recrearlos). No toca lo que ya
     * está en una jornada (EN_PREPARACION), realizado, omitido ni vencido: son historial.
     */
    private void conciliarSerie(List<EventoCalendarioSanitario> existentes, List<CicloPeriodico> ciclos,
                                Set<UUID> ocurrenciasCanceladas) {
        if (existentes.isEmpty()) return;
        Set<String> vigentes = ciclos.stream().map(CicloPeriodico::clave).collect(Collectors.toSet());
        for (EventoCalendarioSanitario e : existentes) {
            boolean vigente = vigentes.contains(e.cicloClave());
            if (e.estado() == EstadoEventoCalendario.CANCELADO) {
                if (vigente) eventos.marcarEstado(e.id(), EstadoEventoCalendario.PROYECTADO, null, null);
            } else if (!vigente) {
                eventos.marcarEstado(e.id(), EstadoEventoCalendario.CANCELADO, null, null);
                if (e.ocurrenciaId() != null) ocurrenciasCanceladas.add(e.ocurrenciaId());
            }
        }
    }

    /** Resuelve la alerta de cada ocurrencia que, tras conciliar, se quedó sin eventos pendientes. */
    private void resolverAlertasSinPendientes(PlanSanitarioItem item, Set<UUID> ocurrenciasCanceladas) {
        MotorAlertas m = alertas.getIfAvailable();
        if (m == null) return;
        for (UUID ocurrenciaId : ocurrenciasCanceladas) {
            if (!eventos.tienePendientes(ocurrenciaId)) {
                m.resolverPorOrigen(item.empresaId(), "EVENTO_CALENDARIO_SANITARIO", ocurrenciaId);
            }
        }
    }

    private LocalDate referenciaPeriodica(ReferenciaCalculoPeriodica tipo, PlanSanitarioItem item, PlanSanitario plan,
                                          CandidatoAnimal a, Map<UUID, LocalDate> ultimasAplicaciones) {
        return switch (tipo) {
            case FECHA_DE_INGRESO -> a.fechaIngreso();
            case FECHA_DE_NACIMIENTO -> a.fechaNacimiento();
            case ULTIMA_APLICACION -> {
                LocalDate ultima = ultimasAplicaciones.get(a.animalId());
                yield ultima != null ? ultima : item.vigenteDesde().atZone(ZONA).toLocalDate();
            }
            case FECHA_INICIAL_DEL_PLAN -> plan.fechaInicio();
            case FECHA_CONFIGURADA -> item.vigenteDesde().atZone(ZONA).toLocalDate();
        };
    }

    /**
     * Última aplicación de cada animal, contando las de todas las versiones de la actividad (misma
     * identidad lógica): al editar una actividad ya usada nace otra fila, y mirar solo la versión
     * actual haría olvidar lo aplicado antes y volver a contar desde la fecha de vigencia.
     */
    private Map<UUID, LocalDate> ultimasAplicaciones(PlanSanitarioItem item) {
        Map<UUID, LocalDate> ultimas = new HashMap<>();
        jdbc.sql("""
                select a.animal_id, max(a.fecha_aplicacion) as ultima
                from aplicacion_sanitaria a join plan_sanitario_item i on i.id = a.plan_item_id
                where i.identidad_logica_id = :identidad and a.estado = 'APLICADO'
                group by a.animal_id
                """).param("identidad", item.identidadLogicaId().toString())
                .query((r, n) -> {
                    ultimas.put(UUID.fromString(r.getString("animal_id")), LocalDate.parse(r.getString("ultima")));
                    return n;
                }).list();
        return ultimas;
    }

    private int procesarFechaProgramada(PlanSanitarioItem item, PlanSanitario plan, LocalDate hoy, int horizonteMeses) {
        if (!(item.modalidadConfig() instanceof ModalidadConfig.FechaProgramadaConfig cfg) || cfg.fechaProgramada() == null) return 0;
        ZoneId zona = cfg.zonaHoraria() == null ? ZONA : ZoneId.of(cfg.zonaHoraria());
        LocalDate fechaDate = cfg.fechaProgramada().atZone(zona).toLocalDate();
        if (fechaDate.isAfter(hoy.plusMonths(horizonteMeses))) return 0;
        int generados = 0;
        Map<UUID, OcurrenciaProgramada> programadas = new LinkedHashMap<>();
        for (CandidatoAnimal a : candidatos(item, plan.propiedadId())) {
            if (!edadElegible(item, a, fechaDate)) continue;
            String ciclo = "FECHA:" + fechaDate;
            acumular(programadas, crear(item, a, ciclo, cfg.fechaProgramada(), cfg.fechaProgramada(),
                    cfg.fechaProgramada(), fechaDate, hoy, ModalidadActividad.FECHA_PROGRAMADA));
            generados++;
        }
        programarAlertas(item, programadas.values());
        return generados;
    }

    private OcurrenciaProgramada crear(PlanSanitarioItem item, CandidatoAnimal a, String ciclo, Instant fechaPrevista,
                                       Instant ventanaDesde, Instant ventanaHasta, LocalDate ventanaDesdeDate, LocalDate hoy,
                                       ModalidadActividad modalidad) {
        String ocurrenciaClave = item.id() + "|" + fechaPrevista + "|" + a.propiedadId() + "|" + a.potreroId() + "|"
                + (a.loteId() == null ? "SIN_LOTE" : a.loteId());
        UUID ocurrenciaId = ocurrencias.crearOUsar(new OcurrenciaCalendarioSanitario(UUID.randomUUID(), item.id(),
                fechaPrevista, a.propiedadId(), a.potreroId(), a.loteId(), ocurrenciaClave, Instant.now(), 0));
        EstadoEventoCalendario estado = hoy.isBefore(ventanaDesdeDate)
                ? EstadoEventoCalendario.PROYECTADO : EstadoEventoCalendario.PROGRAMADO;
        eventos.crearSiNoExiste(new EventoCalendarioSanitario(UUID.randomUUID(), item.empresaId(), item.id(),
                a.animalId(), ciclo, fechaPrevista, ventanaDesde, ventanaHasta, estado, modalidad,
                null, null, null, ocurrenciaId, "NORMAL", Instant.now(), 0));
        encolarSincronizacionExterna(item, a, ocurrenciaId, fechaPrevista);
        return new OcurrenciaProgramada(ocurrenciaId, fechaPrevista, a.propiedadId(), a.potreroId(), a.loteId(), 1);
    }

    private void acumular(Map<UUID, OcurrenciaProgramada> programadas, OcurrenciaProgramada ocurrencia) {
        programadas.merge(ocurrencia.id(), ocurrencia, (actual, nueva) -> actual.conUnAnimalMas());
    }

    /**
     * Outbox transaccional: la fila se confirma o revierte junto con el calendario sanitario.
     * La clave única evita duplicados cuando varios animales pertenecen a la misma ocurrencia.
     */
    private void encolarSincronizacionExterna(PlanSanitarioItem item, CandidatoAnimal a, UUID ocurrenciaId,
                                               Instant fechaPrevista) {
        UUID empresaId = item.empresaId() == null ? EMPRESA_LOCAL : item.empresaId();
        jdbc.sql("""
                insert into cola_sincronizacion_calendario
                    (id,empresa_id,ocurrencia_id,proveedor,operacion,clave_idempotencia,payload)
                select :id,:empresa,:ocurrencia,'GOOGLE_CALENDAR','CREAR',:clave,
                       json_object('ocurrenciaId',:ocurrencia,'actividadId',:actividad,
                                   'fechaPrevista',:fecha,'propiedadId',:propiedad,
                                   'potreroId',:potrero,'loteId',:lote)
                where exists(select 1 from configuracion_calendario_externo
                    where empresa_id=:empresa and proveedor='GOOGLE_CALENDAR'
                      and sincronizacion_automatica=1 and estado<>'DESHABILITADO')
                on conflict(clave_idempotencia) do nothing
                """).param("id",UUID.randomUUID().toString()).param("empresa",empresaId.toString())
                .param("ocurrencia",ocurrenciaId.toString()).param("clave","GOOGLE_CALENDAR:CREAR:"+ocurrenciaId+":v0")
                .param("actividad",item.id().toString()).param("fecha",fechaPrevista.toString())
                .param("propiedad",a.propiedadId().toString()).param("potrero",a.potreroId().toString())
                .param("lote",a.loteId()==null?null:a.loteId().toString()).update();
    }

    /**
     * Una sola alerta por ocurrencia (actividad + fecha + ubicación), no una por animal ni una por
     * horario de aviso: los {@code horariosAviso} quedan en metadata como horarios de notificación.
     * Así una jornada de 60 animales genera 1 alerta grupal en vez de 60.
     */
    private void programarAlertas(PlanSanitarioItem item, Collection<OcurrenciaProgramada> programadas) {
        for (OcurrenciaProgramada p : programadas) programarAlertaOcurrencia(item, p);
    }

    private void programarAlertaOcurrencia(PlanSanitarioItem item, OcurrenciaProgramada p) {
        MotorAlertas m = alertas.getIfAvailable();
        if (m == null) return;
        LocalDate fechaBase = p.fechaPrevista().atZone(ZONA).toLocalDate();
        List<LocalTime> horarios = item.horariosAviso() == null || item.horariosAviso().isEmpty()
                ? List.of(LocalTime.of(8, 0)) : item.horariosAviso();
        LocalTime primerAviso = horarios.stream().min(LocalTime::compareTo).orElse(LocalTime.of(8, 0));
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombreActividad", item.nombre());
        datos.put("fechaProximaAplicacion", fechaBase.toString());
        datos.put("cantidadAnimales", p.cantidadAnimales());
        datos.put("horariosAviso", horarios.stream().map(LocalTime::toString).toList());
        datos.put("propiedadId", p.propiedadId().toString());
        if (p.potreroId() != null) datos.put("potreroId", p.potreroId().toString());
        if (p.loteGanaderoId() != null) datos.put("loteId", p.loteGanaderoId().toString());
        datos.put("eventoReferencia", p.id().toString());
        Instant aviso = fechaBase.minusDays(item.diasAlerta()).atTime(primerAviso).atZone(ZONA).toInstant();
        TipoAlerta tipo = p.fechaPrevista().isBefore(Instant.now())
                ? TipoAlerta.ACTIVIDAD_SANITARIA_VENCIDA : TipoAlerta.ACTIVIDAD_SANITARIA_PROXIMA;
        m.evolucionar(new ProgramarAlertaCommand(item.empresaId(), null, tipo, aviso, p.fechaPrevista(),
                "EVENTO_CALENDARIO_SANITARIO", p.id(), datos),
                java.util.Set.of(TipoAlerta.ACTIVIDAD_SANITARIA_PROXIMA, TipoAlerta.ACTIVIDAD_SANITARIA_VENCIDA));
    }

    private List<CandidatoAnimal> candidatos(PlanSanitarioItem item, UUID propiedadId) {
        StringBuilder sql = new StringBuilder("""
                select a.id as animal_id, a.fecha_nacimiento, a.fecha_nacimiento_estimada, a.fecha_ingreso,
                       a.potrero_actual_id, a.lote_actual_id, p.propiedad_id
                from animal a join potrero p on p.id = a.potrero_actual_id
                where a.estado='ACTIVO'
                """);
        Map<String, Object> params = new HashMap<>();
        if (propiedadId != null) {
            sql.append(" and p.propiedad_id=:propiedad");
            params.put("propiedad", propiedadId.toString());
        }
        if (item.sexoAplicable() != null) {
            sql.append(" and a.sexo=:sexo");
            params.put("sexo", item.sexoAplicable().name());
        }
        if (item.categoriasAplicables() != null && !item.categoriasAplicables().isEmpty()) {
            sql.append(" and a.categoria_actual_id in (:categorias)");
            params.put("categorias", item.categoriasAplicables().stream().map(UUID::toString).toList());
        }
        sql.append(" order by a.id");
        var q = jdbc.sql(sql.toString());
        for (var e : params.entrySet()) q = q.param(e.getKey(), e.getValue());
        return q.query(this::mapCandidato).list();
    }

    private CandidatoAnimal mapCandidato(ResultSet r, int n) throws SQLException {
        String fn = r.getString("fecha_nacimiento");
        String lote = r.getString("lote_actual_id");
        return new CandidatoAnimal(UUID.fromString(r.getString("animal_id")), fn == null ? null : LocalDate.parse(fn),
                r.getBoolean("fecha_nacimiento_estimada"), LocalDate.parse(r.getString("fecha_ingreso")),
                UUID.fromString(r.getString("potrero_actual_id")), lote == null ? null : UUID.fromString(lote),
                UUID.fromString(r.getString("propiedad_id")));
    }

    private int aDias(int valor, UnidadEdadActividad unidad) {
        return unidad.aDias(valor);
    }

    private int aDiasFrecuencia(int valor, UnidadFrecuencia unidad) {
        return switch (unidad) {
            case DIAS -> valor;
            case SEMANAS -> valor * 7;
            case MESES -> valor * 30;
            case ANIOS -> valor * 365;
        };
    }

    /** Una fecha de la serie periódica de un animal, con la clave que la identifica en el calendario. */
    private record CicloPeriodico(String clave, LocalDate fecha, LocalDate ventanaDesde, LocalDate ventanaHasta) {
    }

    private record CandidatoAnimal(UUID animalId, LocalDate fechaNacimiento, boolean fechaNacimientoEstimada,
                                   LocalDate fechaIngreso, UUID potreroId, UUID loteId, UUID propiedadId) {
    }

    /** Una ocurrencia ya proyectada con cuántos animales cayeron en ella en esta corrida. */
    private record OcurrenciaProgramada(UUID id, Instant fechaPrevista, UUID propiedadId, UUID potreroId,
                                        UUID loteGanaderoId, int cantidadAnimales) {
        OcurrenciaProgramada conUnAnimalMas() {
            return new OcurrenciaProgramada(id, fechaPrevista, propiedadId, potreroId, loteGanaderoId,
                    cantidadAnimales + 1);
        }
    }
}
