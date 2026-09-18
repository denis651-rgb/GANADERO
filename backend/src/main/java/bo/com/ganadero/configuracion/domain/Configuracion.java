package bo.com.ganadero.configuracion.domain;
import java.util.UUID;

/**
 * Fila unica de ajustes globales de la app de escritorio. Los campos usan tipos nullable
 * porque este mismo record se usa tanto para leer el estado actual como para representar
 * un patch parcial (un campo null significa "no modificar" en update()).
 */
public record Configuracion(UUID id, String zonaHoraria, String moneda, String unidadPeso, String unidadSuperficie,
 Integer diasAlertaPreparto, Integer diasSinPesaje, Integer diasAlertaDestete,
 Integer diasDiagnosticoPostServicio, Integer diasGestacionEstimada, Boolean comprimirImagenes, Integer calidadImagen,
 String nombreUsuario, boolean pinConfigurado, Integer diasToleranciaPesoCompra, String horaAvisos, long version) {}
