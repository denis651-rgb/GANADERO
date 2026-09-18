package bo.com.ganadero.dashboard.application;

import bo.com.ganadero.alertas.application.AlertaQueryService;
import bo.com.ganadero.alertas.application.CategoriaAlerta;
import bo.com.ganadero.alertas.application.NivelAtencion;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.dashboard.domain.DashboardRepository;
import bo.com.ganadero.dashboard.domain.DashboardResumen;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DashboardService {
    /**
     * Texto de cada tipo de alerta del módulo de alertas en «Atención requerida». Los avisos de pesaje
     * ({@link CategoriaAlerta#PESAJE}) no se listan acá: la pantalla ya muestra el suyo, con botón para
     * registrar el pesaje, y repetirlo sería duplicarlo.
     */
    private static final Map<TipoAlerta, String> ETIQUETAS = Map.ofEntries(
            Map.entry(TipoAlerta.CELO_DETECTADO, "Celos detectados por atender"),
            Map.entry(TipoAlerta.DIAGNOSTICO_PENDIENTE, "Diagnósticos de gestación pendientes"),
            Map.entry(TipoAlerta.PARTO_PROXIMO, "Partos próximos"),
            Map.entry(TipoAlerta.DESTETE_PROXIMO, "Destetes próximos"),
            Map.entry(TipoAlerta.VACUNA_PROXIMA, "Vacunas próximas"),
            Map.entry(TipoAlerta.VACUNA_VENCIDA, "Vacunas vencidas"),
            Map.entry(TipoAlerta.ACTIVIDAD_SANITARIA_PROXIMA, "Actividades sanitarias próximas"),
            Map.entry(TipoAlerta.ACTIVIDAD_SANITARIA_VENCIDA, "Actividades sanitarias vencidas"),
            Map.entry(TipoAlerta.REVISION_SANITARIA_INGRESO, "Revisiones sanitarias de ingreso pendientes"),
            Map.entry(TipoAlerta.RETIRO_CARNE_VIGENTE, "Animales con retiro de carne vigente"),
            Map.entry(TipoAlerta.RETIRO_LECHE_VIGENTE, "Animales con retiro de leche vigente"),
            Map.entry(TipoAlerta.CUARENTENA_POR_FINALIZAR, "Cuarentenas por finalizar"),
            Map.entry(TipoAlerta.CASO_CLINICO_CRITICO, "Casos clínicos críticos"),
            Map.entry(TipoAlerta.RECORDATORIO_SANIDAD, "Recordatorios de sanidad"),
            Map.entry(TipoAlerta.TRATAMIENTO_PROXIMO, "Tratamientos próximos"),
            Map.entry(TipoAlerta.TRATAMIENTO_ATRASADO, "Tratamientos atrasados"),
            Map.entry(TipoAlerta.MOVIMIENTO_PENDIENTE, "Movimientos pendientes"),
            Map.entry(TipoAlerta.INVENTARIO_BAJO, "Inventario bajo"),
            Map.entry(TipoAlerta.SISTEMA_REQUIERE_ATENCION, "El sistema requiere atención"));

    private final DashboardRepository repository;
    private final AlertaQueryService alertasPendientes;
    private final UserContext context;

    public DashboardService(DashboardRepository repository, AlertaQueryService alertasPendientes, UserContext context) {
        this.repository = repository;
        this.alertasPendientes = alertasPendientes;
        this.context = context;
    }

    @Transactional(readOnly = true)
    public DashboardResumen resumen() {
        CurrentUser user = context.requirePermission("DASHBOARD_VER");
        UUID empresa = user.empresaId();
        boolean todas = user.accesoTodasPropiedades();
        var permitidas = user.propiedadesPermitidas();

        long sinPesaje = repository.countAnimalesSinPesaje(empresa, todas, permitidas);
        return new DashboardResumen(
                repository.countAnimales(empresa, todas, permitidas),
                repository.countAnimalesEnPotrero(empresa, todas, permitidas),
                repository.countLotesActivos(empresa, todas, permitidas),
                repository.countPotrerosActivos(empresa, todas, permitidas),
                repository.pesoPromedio(empresa, todas, permitidas),
                repository.countPesajesUltimosDias(empresa, 7, todas, permitidas),
                repository.countMovimientosUltimosDias(empresa, 7, todas, permitidas),
                sinPesaje,
                repository.animalesPorCategoria(empresa, todas, permitidas),
                repository.animalesPorPotrero(empresa, todas, permitidas),
                repository.animalesPorLote(empresa, todas, permitidas),
                repository.pesajesRecientes(empresa, todas, permitidas, 8),
                alertas(user, empresa, todas, permitidas),
                Instant.now());
    }

    /**
     * «Atención requerida»: lo pendiente en los módulos de alertas (sanidad, reproducción, tratamientos,
     * movimientos…) y los potreros inactivos. El aviso de animales sin pesaje lo agrega la pantalla con
     * su botón; la ganancia diaria no se avisa porque no se pesa todos los días.
     */
    private List<DashboardResumen.AlertaBasica> alertas(CurrentUser user, UUID empresa, boolean todas, Set<UUID> permitidas) {
        List<DashboardResumen.AlertaBasica> alertas = new ArrayList<>(alertasDeModulos(user));
        long potrerosInactivos = repository.countPotrerosInactivos(empresa, todas, permitidas);
        if (potrerosInactivos > 0) {
            alertas.add(new DashboardResumen.AlertaBasica("POTREROS_INACTIVOS", "Potreros inactivos", "info", potrerosInactivos));
        }
        return alertas;
    }

    private List<DashboardResumen.AlertaBasica> alertasDeModulos(CurrentUser user) {
        // Quien no puede ver alertas sigue viendo el resto del dashboard.
        if (!user.hasPermission("ALERTA_VER")) return List.of();
        return alertasPendientes.pendientesPorTipo().stream()
                .filter(p -> p.tipo().categoria() != CategoriaAlerta.PESAJE)
                .sorted(Comparator.comparing(AlertaQueryService.PendientesPorTipo::nivel, Comparator.reverseOrder())
                        .thenComparing(AlertaQueryService.PendientesPorTipo::total, Comparator.reverseOrder())
                        .thenComparing(p -> p.tipo().name()))
                .map(p -> new DashboardResumen.AlertaBasica(p.tipo().name(), etiqueta(p.tipo()),
                        severidad(p.nivel()), p.total()))
                .toList();
    }

    static String etiqueta(TipoAlerta tipo) {
        return ETIQUETAS.getOrDefault(tipo, tipo.name());
    }

    /** El dashboard pinta tres niveles: lo urgente en rojo, las advertencias en ámbar y lo informativo en azul. */
    static String severidad(NivelAtencion nivel) {
        return switch (nivel) {
            case URGENTE -> "danger";
            case ADVERTENCIA -> "warning";
            case INFORMATIVO -> "info";
        };
    }
}
