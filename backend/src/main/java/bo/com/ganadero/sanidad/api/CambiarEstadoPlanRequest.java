package bo.com.ganadero.sanidad.api; import bo.com.ganadero.sanidad.domain.EstadoPlanSanitario; import jakarta.validation.constraints.*;
public record CambiarEstadoPlanRequest(@NotNull EstadoPlanSanitario estado,@PositiveOrZero long version) {}
