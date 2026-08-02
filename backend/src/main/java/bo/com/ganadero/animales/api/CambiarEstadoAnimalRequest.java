package bo.com.ganadero.animales.api; import bo.com.ganadero.animales.domain.EstadoAnimal; import jakarta.validation.constraints.*;
public record CambiarEstadoAnimalRequest(@NotNull EstadoAnimal estado,@NotBlank @Size(max=1000) String motivo,@NotNull Long version) {}
