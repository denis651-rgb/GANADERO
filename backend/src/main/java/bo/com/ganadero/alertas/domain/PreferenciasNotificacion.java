package bo.com.ganadero.alertas.domain;

import bo.com.ganadero.alertas.application.CategoriaAlerta;
import bo.com.ganadero.alertas.application.TipoAlerta;

import java.util.UUID;

public record PreferenciasNotificacion(
        UUID empresaId,
        UUID usuarioId,
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

    public boolean permite(TipoAlerta tipo) {
        if (tipo == TipoAlerta.CASO_CLINICO_CRITICO && !casosCriticos) {
            return false;
        }
        return permite(tipo.categoria());
    }

    public boolean permite(CategoriaAlerta categoria) {
        return switch (categoria) {
            case REPRODUCCION -> reproduccion;
            case SANIDAD -> sanidad;
            case TRATAMIENTO -> tratamientos;
            case PESAJE -> pesajes;
            case MOVIMIENTO -> movimientos;
            case INVENTARIO -> inventario;
            case SISTEMA -> sistema;
        };
    }
}
