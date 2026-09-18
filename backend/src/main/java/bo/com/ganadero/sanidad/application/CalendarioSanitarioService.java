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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    private static final int LIMITE_ANIMALES = 500;
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
        for (CandidatoAnimal a : candidatos(item, plan.propiedadId())) {
            if (a.fechaNacimiento() == null) continue; // EXCLUIR e INCLUIR_MANUAL ambos evitan la generación automática
            if (a.fechaNacimientoEstimada() && cfg.politicaEdadEstimada() == PoliticaEdadEstimada.EXCLUIR) continue;
            int edadObjetivoDias = aDias(cfg.edadObjetivoValor(), cfg.edadUnidad());
            LocalDate fechaObjetivo = a.fechaNacimiento().plusDays(edadObjetivoDias);
            if (fechaObjetivo.isAfter(limite)) continue; // fuera del horizonte de proyección
            LocalDate ventanaDesde = fechaObjetivo.minusDays(cfg.ventanaAnticipadaDias());
            LocalDate ventanaHasta = fechaObjetivo.plusDays(cfg.ventanaPosteriorDias());
            String ciclo = "EDAD:" + edadObjetivoDias;
            Instant fechaPrevista = fechaObjetivo.atTime(item.horaEjecucion()).atZone(ZONA).toInstant();
            acumular(programadas, crear(item, a, ciclo, fechaPrevista, ventanaDesde.atStartOfDay(ZONA).toInstant(),
                    ventanaHasta.atStartOfDay(ZONA).toInstant(), ventanaDesde, hoy, ModalidadActividad.POR_EDAD));
            generados++;
        }
        programarAlertas(item, programadas.values());
        return generados;
    }

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
        for (CandidatoAnimal a : candidatos(item, plan.propiedadId())) {
            LocalDate referencia = referenciaPeriodica(cfg.referenciaCalculo(), item, plan, a);
            if (referencia == null) continue;
            for (int ciclo = 1; ciclo <= maxCiclos; ciclo++) {
                LocalDate proxima = referencia.plusDays((long) frecuenciaDias * ciclo);
                if (proxima.isAfter(limite)) break; // ciclos futuros son estrictamente crecientes
                LocalDate ventanaDesde = proxima.minusDays(cfg.toleranciaAnticipadaDias());
                LocalDate ventanaHasta = proxima.plusDays(cfg.toleranciaPosteriorDias());
                String claveCiclo = "PERIODO:" + proxima;
                Instant fechaPrevista = proxima.atTime(item.horaEjecucion()).atZone(ZONA).toInstant();
                acumular(programadas, crear(item, a, claveCiclo, fechaPrevista, ventanaDesde.atStartOfDay(ZONA).toInstant(),
                        ventanaHasta.atStartOfDay(ZONA).toInstant(), ventanaDesde, hoy, ModalidadActividad.PERIODICA));
                generados++;
            }
        }
        programarAlertas(item, programadas.values());
        return generados;
    }

    private LocalDate referenciaPeriodica(ReferenciaCalculoPeriodica tipo, PlanSanitarioItem item, PlanSanitario plan, CandidatoAnimal a) {
        return switch (tipo) {
            case FECHA_DE_INGRESO -> a.fechaIngreso();
            case FECHA_DE_NACIMIENTO -> a.fechaNacimiento();
            case ULTIMA_APLICACION -> ultimaAplicacion(item.id(), a.animalId())
                    .orElseGet(() -> item.vigenteDesde().atZone(ZONA).toLocalDate());
            case FECHA_INICIAL_DEL_PLAN -> plan.fechaInicio();
            case FECHA_CONFIGURADA -> item.vigenteDesde().atZone(ZONA).toLocalDate();
        };
    }

    private Optional<LocalDate> ultimaAplicacion(UUID itemId, UUID animalId) {
        return jdbc.sql("""
                select max(fecha_aplicacion) from aplicacion_sanitaria
                where plan_item_id=:item and animal_id=:animal and estado='APLICADO'
                """).param("item", itemId.toString()).param("animal", animalId.toString())
                .query(String.class).optional().map(LocalDate::parse);
    }

    private int procesarFechaProgramada(PlanSanitarioItem item, PlanSanitario plan, LocalDate hoy, int horizonteMeses) {
        if (!(item.modalidadConfig() instanceof ModalidadConfig.FechaProgramadaConfig cfg) || cfg.fechaProgramada() == null) return 0;
        ZoneId zona = cfg.zonaHoraria() == null ? ZONA : ZoneId.of(cfg.zonaHoraria());
        LocalDate fechaDate = cfg.fechaProgramada().atZone(zona).toLocalDate();
        if (fechaDate.isAfter(hoy.plusMonths(horizonteMeses))) return 0;
        int generados = 0;
        Map<UUID, OcurrenciaProgramada> programadas = new LinkedHashMap<>();
        for (CandidatoAnimal a : candidatos(item, plan.propiedadId())) {
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
        sql.append(" limit ").append(LIMITE_ANIMALES);
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
        return switch (unidad) {
            case DIAS -> valor;
            case MESES -> valor * 30;
            case ANIOS -> valor * 365;
        };
    }

    private int aDiasFrecuencia(int valor, UnidadFrecuencia unidad) {
        return switch (unidad) {
            case DIAS -> valor;
            case SEMANAS -> valor * 7;
            case MESES -> valor * 30;
            case ANIOS -> valor * 365;
        };
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
