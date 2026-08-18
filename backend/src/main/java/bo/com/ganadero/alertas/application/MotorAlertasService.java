package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.Alerta;
import bo.com.ganadero.alertas.domain.AlertaRepository;
import bo.com.ganadero.alertas.domain.EstadoAlerta;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MotorAlertasService implements MotorAlertas {
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/La_Paz");
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZONA_NEGOCIO);

    private final AlertaRepository repository;

    public MotorAlertasService(AlertaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public UUID programar(ProgramarAlertaCommand command) {
        Preparacion preparacion = preparar(command);
        return repository.programar(preparacion.alerta()).id();
    }

    @Override
    @Transactional
    public UUID evolucionar(ProgramarAlertaCommand command, Set<TipoAlerta> tiposAnteriores) {
        Preparacion preparacion = preparar(command);
        return repository.evolucionarOrigen(preparacion.alerta(), tiposAnteriores)
                .orElseGet(() -> repository.programar(preparacion.alerta())).id();
    }

    private Preparacion preparar(ProgramarAlertaCommand command) {
        Map<String, Object> metadata = command.metadata() == null
                ? new HashMap<>() : new HashMap<>(command.metadata());
        Instant vencimiento = instant(metadata.remove("fechaVencimiento"));
        Plantilla plantilla = plantilla(command.tipo(), vencimiento == null
                ? command.fechaProgramada() : vencimiento, metadata);
        Alerta alerta = new Alerta(UUID.randomUUID(), command.empresaId(), command.animalId(), command.tipo(),
                plantilla.titulo(), plantilla.mensaje(), plantilla.severidad(), command.fechaProgramada(),
                vencimiento, command.origenTipo(), command.origenId(), EstadoAlerta.PROGRAMADA, metadata,
                null, null, null, null, null, null, null, 0, null, null, null,
                claveIdempotencia(command, metadata));
        return new Preparacion(alerta);
    }

    @Override
    @Transactional
    public void resolverPorOrigen(UUID empresaId, String origenTipo, UUID origenId) {
        repository.resolverOrigen(empresaId, origenTipo, origenId, null);
    }

    @Override
    @Transactional
    public void cancelarPorOrigen(UUID empresaId, String origenTipo, UUID origenId, String motivo) {
        repository.cancelarOrigen(empresaId, origenTipo, origenId, motivo);
    }

    private Instant instant(Object value) {
        try {
            return value == null ? null : Instant.parse(value.toString());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Plantilla plantilla(TipoAlerta tipo, Instant fecha, Map<String, Object> metadata) {
        String fechaTexto = FORMATO_FECHA.format(fecha);
        String animal = etiquetaAnimal(metadata);
        return switch (tipo) {
            case CELO_DETECTADO -> new Plantilla("Celo detectado",
                    "Se detectó un celo que requiere seguimiento reproductivo.", SeveridadAlerta.INFO);
            case DIAGNOSTICO_PENDIENTE -> new Plantilla("Diagnóstico de gestación pendiente",
                    animal + " tiene un diagnóstico de gestación pendiente para el " + fechaTexto,
                    SeveridadAlerta.WARNING);
            case PARTO_PROXIMO -> new Plantilla("Parto próximo",
                    animal + " tiene parto estimado para el " + fechaTexto, SeveridadAlerta.WARNING);
            case DESTETE_PROXIMO -> new Plantilla("Destete próximo",
                    "El destete está previsto para " + fechaTexto, SeveridadAlerta.INFO);
            case VACUNA_PROXIMA -> new Plantilla("Vacuna próxima",
                    animal + " tiene una vacunación prevista para el " + fechaTexto, severidadVacuna(metadata));
            case VACUNA_VENCIDA -> new Plantilla("Vacuna vencida",
                    "La vacunación de " + animal + " está vencida.", severidadVacuna(metadata));
            case TRATAMIENTO_PROXIMO -> new Plantilla("Tratamiento próximo",
                    "Existe una aplicación programada para " + fechaTexto, SeveridadAlerta.URGENTE);
            case TRATAMIENTO_ATRASADO -> new Plantilla("Tratamiento atrasado",
                    "El tratamiento de " + animal + " está atrasado.", SeveridadAlerta.URGENTE);
            case RETIRO_CARNE_VIGENTE -> new Plantilla("Retiro de carne vigente",
                    "El periodo de retiro de carne finaliza el " + fechaTexto, SeveridadAlerta.WARNING);
            case RETIRO_LECHE_VIGENTE -> new Plantilla("Retiro de leche vigente",
                    "El periodo de retiro de leche finaliza el " + fechaTexto, SeveridadAlerta.WARNING);
            case PESAJE_ATRASADO -> new Plantilla("Pesaje atrasado",
                    animal + " lleva " + metadata.getOrDefault("diasSinPesaje", "varios")
                            + " días sin pesaje.", SeveridadAlerta.WARNING);
            case CASO_CLINICO_CRITICO -> new Plantilla("Caso clínico crítico",
                    "Se registró un caso clínico crítico que requiere atención inmediata.", SeveridadAlerta.CRITICA);
            case RECORDATORIO_SANIDAD -> new Plantilla(
                    String.valueOf(metadata.getOrDefault("tituloPersonalizado", "Recordatorio de sanidad")),
                    String.valueOf(metadata.getOrDefault("mensajePersonalizado", "Existe una actividad sanitaria programada.")),
                    severidad(metadata));
            case CUARENTENA_POR_FINALIZAR -> new Plantilla("Cuarentena por finalizar",
                    "La cuarentena finaliza el " + fechaTexto, SeveridadAlerta.WARNING);
            case MOVIMIENTO_PENDIENTE -> new Plantilla("Movimiento pendiente",
                    "Existe un movimiento ganadero que requiere seguimiento.", SeveridadAlerta.WARNING);
            case INVENTARIO_BAJO -> new Plantilla("Inventario bajo",
                    "Un insumo alcanzó el nivel mínimo configurado.", SeveridadAlerta.WARNING);
            case SISTEMA_REQUIERE_ATENCION -> new Plantilla("Sistema requiere atención",
                    "Se detectó una situación técnica que requiere revisión.", SeveridadAlerta.URGENTE);
        };
    }

    private SeveridadAlerta severidadVacuna(Map<String, Object> metadata) {
        int diasRestantes;
        try {
            diasRestantes = Integer.parseInt(String.valueOf(metadata.getOrDefault("diasRestantes", 7)));
        } catch (NumberFormatException ignored) {
            diasRestantes = 7;
        }
        if (diasRestantes <= -7) return SeveridadAlerta.CRITICA;
        if (diasRestantes <= 0) return SeveridadAlerta.URGENTE;
        if (diasRestantes <= 3) return SeveridadAlerta.WARNING;
        return SeveridadAlerta.INFO;
    }

    private SeveridadAlerta severidad(Map<String, Object> metadata) {
        try { return SeveridadAlerta.valueOf(String.valueOf(metadata.getOrDefault("severidad", "WARNING"))); }
        catch (IllegalArgumentException ignored) { return SeveridadAlerta.WARNING; }
    }

    private String etiquetaAnimal(Map<String, Object> metadata) {
        Object nombre = metadata.get("animalNombre");
        Object codigo = metadata.get("animalCodigo");
        if (nombre != null && !nombre.toString().isBlank()) return nombre.toString();
        if (codigo != null && !codigo.toString().isBlank()) return codigo.toString();
        return "El animal";
    }

    private String claveIdempotencia(ProgramarAlertaCommand command, Map<String, Object> metadata) {
        Object referencia = metadata.get("eventoReferencia");
        String evento = referencia == null || referencia.toString().isBlank()
                ? command.origenId().toString() : referencia.toString().trim().toUpperCase();
        return String.join("|", command.empresaId().toString(), command.tipo().name(),
                command.origenTipo().trim().toUpperCase(), command.origenId().toString(), evento);
    }

    private record Plantilla(String titulo, String mensaje, SeveridadAlerta severidad) {}
    private record Preparacion(Alerta alerta) {}
}
