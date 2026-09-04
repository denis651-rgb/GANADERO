package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;
import bo.com.ganadero.shared.db.Rows;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

/**
 * Motor de calendario proyectado por animal (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md,
 * secciones 3 y 5). A diferencia de ProcesarAlertasVacunacionService (que solo mira
 * aplicaciones YA hechas para calcular el refuerzo), este servicio mira hacia adelante:
 * para cada animal activo y cada item de plan sin ninguna AplicacionSanitaria todavia
 * (ni aplicada ni declarada), proyecta si deberia haber una y de que tipo, segun si el
 * animal nacio en la finca (fecha_nacimiento real) o fue comprado sin ese dato.
 *
 * Sigue el mismo patron que ProcesarAlertasVacunacionService: JdbcClient con SQL directo
 * cruzando tablas de animales/sanidad, sin importar repositorios de otros modulos (asi
 * esta resuelto hoy el acceso de lectura entre modulos en este proyecto).
 *
 * Alcance deliberado de esta fase: solo items tipo_actividad = VACUNACION. Tratar
 * cualquier tipo de item con VACUNA_PROXIMA/VACUNA_VENCIDA reintroduciria exactamente el
 * error que corrigio la Fase 1 (aftosa/VIGILANCIA_EPIDEMIOLOGICA no es un ciclo de vacuna
 * con fecha fija). Extender a otros tipos de actividad queda para una fase futura.
 */
@Service
public class ProyectarCalendarioSanitarioService {
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");
    private static final Set<TipoAlerta> TIPOS_PROYECCION =
            Set.of(TipoAlerta.VACUNA_PROXIMA, TipoAlerta.VACUNA_VENCIDA, TipoAlerta.REVISION_SANITARIA_INGRESO);
    private static final Map<TipoActividadSanitaria, String> ETIQUETAS_ACTIVIDAD = Map.of(
            TipoActividadSanitaria.VACUNACION, "vacunación",
            TipoActividadSanitaria.DESPARASITACION, "desparasitación",
            TipoActividadSanitaria.VITAMINIZACION, "vitaminización",
            TipoActividadSanitaria.CONTROL, "control",
            TipoActividadSanitaria.PRUEBA_DIAGNOSTICA, "prueba diagnóstica",
            TipoActividadSanitaria.OTRO, "la actividad sanitaria",
            TipoActividadSanitaria.VIGILANCIA_EPIDEMIOLOGICA, "vigilancia epidemiológica");

    private final JdbcClient jdbc;
    private final MotorAlertas alertas;

    public ProyectarCalendarioSanitarioService(JdbcClient jdbc, MotorAlertas alertas) {
        this.jdbc = jdbc;
        this.alertas = alertas;
    }

    @Transactional
    public int procesar() {
        return procesar(LocalDate.now(ZONA_NEGOCIO));
    }

    int procesar(LocalDate hoy) {
        List<AnimalItemPendiente> pendientes = jdbc.sql("""
                        select an.id as animal_id, an.codigo, an.nombre,
                               an.fecha_nacimiento, an.fecha_nacimiento_estimada, an.origen, an.fecha_ingreso, cat.edad_min_meses,
                               i.id as item_id, i.tipo_actividad, i.edad_min_dias, i.edad_max_dias, i.dias_alerta
                        from animal an
                        join categoria_animal cat on cat.id = an.categoria_actual_id
                        join plan_sanitario p on p.estado = 'ACTIVO'
                        join plan_sanitario_item i
                             on i.plan_id = p.id
                            and i.activo = 1
                            and i.tipo_actividad = 'VACUNACION'
                            and i.edad_min_dias is not null
                            and (i.categoria_animal_id is null or i.categoria_animal_id = an.categoria_actual_id)
                            and (i.sexo_aplicable is null or i.sexo_aplicable = an.sexo)
                        where an.estado = 'ACTIVO'
                          and not exists (
                              select 1 from aplicacion_sanitaria a
                              where a.animal_id = an.id and a.plan_item_id = i.id
                          )
                        limit 500
                        """)
                .query((rs, rowNum) -> new AnimalItemPendiente(
                        Rows.uuid(rs, "animal_id"), rs.getString("codigo"), rs.getString("nombre"),
                        rs.getString("fecha_nacimiento") == null ? null : LocalDate.parse(rs.getString("fecha_nacimiento")),
                        LocalDate.parse(rs.getString("fecha_ingreso")), rs.getBoolean("fecha_nacimiento_estimada"), rs.getString("origen"),
                        (Integer) rs.getObject("edad_min_meses"),
                        Rows.uuid(rs, "item_id"), TipoActividadSanitaria.valueOf(rs.getString("tipo_actividad")),
                        rs.getInt("edad_min_dias"), (Integer) rs.getObject("edad_max_dias"), rs.getInt("dias_alerta")))
                .list();

        int procesados = 0;
        Instant ahora = Instant.now();
        for (AnimalItemPendiente item : pendientes) {
            if (item.fechaNacimiento() != null && !item.fechaNacimientoEstimada() && "NACIDO".equals(item.origen())) {
                if (proyectarNacidoEnFinca(item, hoy, ahora)) procesados++;
            } else {
                if (proyectarCompradoSinHistorial(item, hoy, ahora)) procesados++;
            }
        }
        return procesados;
    }

    /** 3.1: fecha_nacimiento real — la ventana es inequivoca, VACUNA_PROXIMA/VACUNA_VENCIDA de siempre. */
    private boolean proyectarNacidoEnFinca(AnimalItemPendiente item, LocalDate hoy, Instant ahora) {
        LocalDate fechaEntradaVentana = item.fechaNacimiento().plusDays(item.edadMinDias());
        int diasRestantes = Math.toIntExact(ChronoUnit.DAYS.between(hoy, fechaEntradaVentana));
        if (diasRestantes > item.diasAlerta()) return false; // todavia falta demasiado, no corresponde alertar aun
        TipoAlerta tipo = diasRestantes <= 0 ? TipoAlerta.VACUNA_VENCIDA : TipoAlerta.VACUNA_PROXIMA;

        Map<String, Object> metadata = datosAnimal(item);
        metadata.put("diasRestantes", diasRestantes);
        metadata.put("fechaProximaAplicacion", fechaEntradaVentana.toString());
        metadata.put("eventoReferencia", fechaEntradaVentana.toString());
        Instant vencimiento = fechaEntradaVentana.atStartOfDay(ZONA_NEGOCIO).toInstant();
        alertas.evolucionar(new ProgramarAlertaCommand(null, item.animalId(), tipo, ahora, vencimiento,
                "PROYECCION_SANITARIA", origenId(item), metadata), TIPOS_PROYECCION);
        return true;
    }

    /**
     * 3.3: sin fecha_nacimiento (tipicamente comprado). fechaReferencia se estima con
     * edad_min_meses de la categoria asignada + fecha_ingreso — aproximado a proposito
     * (seccion 3.3a del documento): no hay forma de saber la edad exacta de un animal que
     * entro sin certificado, pero alcanza para no perder la ventana de uno que ya entra
     * "vaquillona". La alerta es inmediata desde el ingreso (no espera dias_alerta) porque
     * la incertidumbre sobre su historial existe desde el dia uno, no se acerca con el tiempo.
     */
    private boolean proyectarCompradoSinHistorial(AnimalItemPendiente item, LocalDate hoy, Instant ahora) {
        if (hoy.isBefore(item.fechaIngreso())) return false;
        int edadMinMeses = item.edadMinMesesCategoria() == null ? 0 : item.edadMinMesesCategoria();
        LocalDate fechaReferencia = item.fechaNacimiento() != null ? item.fechaNacimiento() : item.fechaIngreso().minusDays(edadMinMeses * 30L);
        LocalDate fechaEntradaVentana = fechaReferencia.plusDays(item.edadMinDias());
        boolean dentroDeVentana = !hoy.isBefore(fechaEntradaVentana) && (item.edadMaxDias() == null
                || !hoy.isAfter(fechaReferencia.plusDays(item.edadMaxDias())));

        Map<String, Object> metadata = datosAnimal(item);
        metadata.put("dentroDeVentana", dentroDeVentana);
        metadata.put("actividad", ETIQUETAS_ACTIVIDAD.getOrDefault(item.tipoActividad(), "la actividad sanitaria"));
        Instant vencimiento = fechaEntradaVentana.atStartOfDay(ZONA_NEGOCIO).toInstant();
        alertas.evolucionar(new ProgramarAlertaCommand(null, item.animalId(), TipoAlerta.REVISION_SANITARIA_INGRESO,
                ahora, vencimiento, "PROYECCION_SANITARIA", origenId(item), metadata), TIPOS_PROYECCION);
        return true;
    }

    private Map<String, Object> datosAnimal(AnimalItemPendiente item) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("animalCodigo", item.codigo());
        if (item.nombre() != null && !item.nombre().isBlank()) metadata.put("animalNombre", item.nombre());
        return metadata;
    }

    /**
     * origenId deterministico por animal+item: si el job corre todos los dias, evolucionar()
     * actualiza la misma alerta en vez de crear una nueva cada vez.
     */
    private UUID origenId(AnimalItemPendiente item) {
        return UUID.nameUUIDFromBytes((item.animalId() + ":" + item.itemId()).getBytes(StandardCharsets.UTF_8));
    }

    private record AnimalItemPendiente(UUID animalId, String codigo, String nombre, LocalDate fechaNacimiento,
                                       LocalDate fechaIngreso, boolean fechaNacimientoEstimada, String origen, Integer edadMinMesesCategoria, UUID itemId,
                                       TipoActividadSanitaria tipoActividad, int edadMinDias, Integer edadMaxDias,
                                       int diasAlerta) {}
}
