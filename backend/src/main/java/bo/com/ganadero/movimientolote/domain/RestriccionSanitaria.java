package bo.com.ganadero.movimientolote.domain;

public record RestriccionSanitaria(String tipo, SeveridadRestriccion severidad, String mensaje) {
}
