package bo.com.ganadero.sanidad.api; import jakarta.validation.constraints.PositiveOrZero;
public record CambiarActivoRequest(boolean activo,@PositiveOrZero long version) {}
