package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.sanidad.domain.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
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
 * <p>Simplificaciones documentadas: para PERIODICA, cuando la referencia es
 * {@code FECHA_CONFIGURADA} o {@code ULTIMA_APLICACION} sin ningún antecedente todavía, se usa
 * la fecha de vigencia de la actividad como semilla (no existe un campo separado de "fecha
 * configurada" en este modelo). La repetición de FECHA_PROGRAMADA ({@code reglaRepeticion}) no
 * está implementada — sólo se genera la fecha única configurada.</p>
 */
@Service
public class CalendarioSanitarioService {
    private static final ZoneId ZONA = ZoneId.of("America/La_Paz");
    private static final int LIMITE_ANIMALES = 500;

    private final SanidadRepository planes;
    private final JdbcClient jdbc;
    private final EventoCalendarioSanitarioRepository eventos;
    private final ObjectProvider<MotorAlertas> alertas;

    public CalendarioSanitarioService(SanidadRepository planes, JdbcClient jdbc,
                                      EventoCalendarioSanitarioRepository eventos, ObjectProvider<MotorAlertas> alertas) {
        this.planes = planes;
        this.jdbc = jdbc;
        this.eventos = eventos;
        this.alertas = alertas;
    }

    @Transactional
    public int procesar() {
        marcarVencidos();
        int generados = 0;
        LocalDate hoy = LocalDate.now(ZONA);
        for (PlanSanitario plan : planes.planes(null)) {
            if (plan.estado() != EstadoPlanSanitario.ACTIVO) continue;
            for (PlanSanitarioItem item : planes.items(plan.id(), null, false)) {
                if (!item.activo() || item.vigenteHasta() != null) continue;
                generados += switch (item.modalidad()) {
                    case POR_EDAD -> procesarPorEdad(item, hoy);
                    case PERIODICA -> procesarPeriodica(item, plan, hoy);
                    case FECHA_PROGRAMADA -> procesarFechaProgramada(item, hoy);
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

    private int procesarPorEdad(PlanSanitarioItem item, LocalDate hoy) {
        if (!(item.modalidadConfig() instanceof ModalidadConfig.PorEdadConfig cfg)) return 0;
        int generados = 0;
        for (CandidatoAnimal a : candidatos(item)) {
            if (a.fechaNacimiento() == null) continue; // EXCLUIR e INCLUIR_MANUAL ambos evitan la generación automática
            if (a.fechaNacimientoEstimada() && cfg.politicaEdadEstimada() == PoliticaEdadEstimada.EXCLUIR) continue;
            int edadObjetivoDias = aDias(cfg.edadObjetivoValor(), cfg.edadUnidad());
            LocalDate fechaObjetivo = a.fechaNacimiento().plusDays(edadObjetivoDias);
            LocalDate ventanaDesde = fechaObjetivo.minusDays(cfg.ventanaAnticipadaDias());
            if (hoy.isBefore(ventanaDesde)) continue;
            LocalDate ventanaHasta = fechaObjetivo.plusDays(cfg.ventanaPosteriorDias());
            String ciclo = "EDAD:" + edadObjetivoDias;
            crear(item, a.animalId(), ciclo, fechaObjetivo, ventanaDesde, ventanaHasta, ModalidadActividad.POR_EDAD);
            generados++;
        }
        return generados;
    }

    private int procesarPeriodica(PlanSanitarioItem item, PlanSanitario plan, LocalDate hoy) {
        if (!(item.modalidadConfig() instanceof ModalidadConfig.PeriodicaConfig cfg)) return 0;
        int generados = 0;
        int frecuenciaDias = aDiasFrecuencia(cfg.frecuenciaValor(), cfg.frecuenciaUnidad());
        for (CandidatoAnimal a : candidatos(item)) {
            LocalDate referencia = referenciaPeriodica(cfg.referenciaCalculo(), item, plan, a);
            if (referencia == null) continue;
            LocalDate proxima = referencia.plusDays(frecuenciaDias);
            if (hoy.isBefore(proxima.minusDays(cfg.toleranciaAnticipadaDias()))) continue;
            LocalDate ventanaDesde = proxima.minusDays(cfg.toleranciaAnticipadaDias());
            LocalDate ventanaHasta = proxima.plusDays(cfg.toleranciaPosteriorDias());
            String ciclo = "PERIODO:" + proxima;
            crear(item, a.animalId(), ciclo, proxima, ventanaDesde, ventanaHasta, ModalidadActividad.PERIODICA);
            generados++;
        }
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

    private int procesarFechaProgramada(PlanSanitarioItem item, LocalDate hoy) {
        if (!(item.modalidadConfig() instanceof ModalidadConfig.FechaProgramadaConfig cfg) || cfg.fechaProgramada() == null) return 0;
        ZoneId zona = cfg.zonaHoraria() == null ? ZONA : ZoneId.of(cfg.zonaHoraria());
        LocalDate fecha = cfg.fechaProgramada().atZone(zona).toLocalDate();
        int generados = 0;
        for (CandidatoAnimal a : candidatos(item)) {
            String ciclo = "FECHA:" + fecha;
            crear(item, a.animalId(), ciclo, fecha, fecha, fecha, ModalidadActividad.FECHA_PROGRAMADA);
            generados++;
        }
        return generados;
    }

    private void crear(PlanSanitarioItem item, UUID animalId, String ciclo, LocalDate prevista, LocalDate ventanaDesde,
                       LocalDate ventanaHasta, ModalidadActividad modalidad) {
        eventos.crearSiNoExiste(new EventoCalendarioSanitario(UUID.randomUUID(), item.empresaId(), item.id(), animalId,
                ciclo, prevista.atStartOfDay(ZONA).toInstant(), ventanaDesde.atStartOfDay(ZONA).toInstant(),
                ventanaHasta.atStartOfDay(ZONA).toInstant(), EstadoEventoCalendario.PROGRAMADO, modalidad,
                null, null, null, "NORMAL", Instant.now(), 0));
        programarAlertas(item, animalId, prevista);
    }

    private void programarAlertas(PlanSanitarioItem item, UUID animalId, LocalDate fechaPrevista) {
        MotorAlertas m = alertas.getIfAvailable();
        if (m == null) return;
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombreActividad", item.nombre());
        datos.put("fechaProximaAplicacion", fechaPrevista.toString());
        datos.put("eventoReferencia", item.id() + ":" + animalId + ":" + fechaPrevista);
        UUID origenId = UUID.nameUUIDFromBytes((item.id() + ":" + animalId + ":" + fechaPrevista)
                .getBytes(StandardCharsets.UTF_8));
        Instant vencimiento = fechaPrevista.atStartOfDay(ZONA).toInstant();
        Instant aviso = fechaPrevista.minusDays(item.diasAlerta()).atStartOfDay(ZONA).toInstant();
        m.evolucionar(new ProgramarAlertaCommand(item.empresaId(), animalId, TipoAlerta.ACTIVIDAD_SANITARIA_PROXIMA,
                aviso, vencimiento, "EVENTO_CALENDARIO_SANITARIO", origenId, datos),
                java.util.Set.of(TipoAlerta.ACTIVIDAD_SANITARIA_PROXIMA, TipoAlerta.ACTIVIDAD_SANITARIA_VENCIDA));
    }

    private List<CandidatoAnimal> candidatos(PlanSanitarioItem item) {
        StringBuilder sql = new StringBuilder("""
                select id as animal_id, fecha_nacimiento, fecha_nacimiento_estimada, fecha_ingreso
                from animal where estado='ACTIVO'
                """);
        Map<String, Object> params = new HashMap<>();
        if (item.sexoAplicable() != null) {
            sql.append(" and sexo=:sexo");
            params.put("sexo", item.sexoAplicable().name());
        }
        if (item.categoriasAplicables() != null && !item.categoriasAplicables().isEmpty()) {
            sql.append(" and categoria_actual_id in (:categorias)");
            params.put("categorias", item.categoriasAplicables().stream().map(UUID::toString).toList());
        }
        sql.append(" limit ").append(LIMITE_ANIMALES);
        var q = jdbc.sql(sql.toString());
        for (var e : params.entrySet()) q = q.param(e.getKey(), e.getValue());
        return q.query(this::mapCandidato).list();
    }

    private CandidatoAnimal mapCandidato(ResultSet r, int n) throws SQLException {
        String fn = r.getString("fecha_nacimiento");
        return new CandidatoAnimal(UUID.fromString(r.getString("animal_id")), fn == null ? null : LocalDate.parse(fn),
                r.getBoolean("fecha_nacimiento_estimada"), LocalDate.parse(r.getString("fecha_ingreso")));
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
                                   LocalDate fechaIngreso) {
    }
}
