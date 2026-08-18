package bo.com.ganadero.alertas.api;

public record PreferenciasNotificacionRequest(
        boolean reproduccion,
        boolean sanidad,
        boolean tratamientos,
        boolean pesajes,
        boolean movimientos,
        boolean inventario,
        boolean sistema,
        boolean casosCriticos,
        boolean criticas,
        boolean urgentes,
        boolean recordatorios) {
}
