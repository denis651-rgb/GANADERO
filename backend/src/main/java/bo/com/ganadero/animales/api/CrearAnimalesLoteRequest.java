package bo.com.ganadero.animales.api;
import bo.com.ganadero.animales.application.AnimalCommand;
import bo.com.ganadero.animales.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Alta masiva de animales comprados (docs/backend/PLAN_SANITARIO_SANTA_CRUZ.md, seccion 7):
 * datos comunes del lote de compra una sola vez, y por fila solo lo que varia por animal.
 * origen queda fijo en COMPRADO — este endpoint es exclusivamente para ingreso por compra.
 */
public record CrearAnimalesLoteRequest(
        @NotNull UUID razaPrincipalId,
        @NotNull PropositoAnimal proposito,
        @NotNull UUID propiedadActualId,
        @NotNull UUID potreroActualId,
        LocalDate fechaIngreso,
        @PositiveOrZero BigDecimal precioAdquisicion,
        @NotEmpty(message = "El lote debe incluir al menos un animal") @Size(max = 200)
        List<@Valid AnimalLoteItemRequest> animales) {

    public record AnimalLoteItemRequest(
            @Size(max = 60) String codigo,
            @Size(max = 160) String nombre,
            @NotNull SexoAnimal sexo,
            @NotNull UUID categoriaActualId,
            String color,
            LocalDate fechaNacimiento,
            Boolean fechaNacimientoEstimada,
            @Positive BigDecimal pesoIngresoKg,
            Boolean pesoIngresoEstimado,
            @DecimalMin("1.0") @DecimalMax("5.0") BigDecimal condicionCorporalActual,
            String observaciones) {

        AnimalCommand command(CrearAnimalesLoteRequest lote) {
            return new AnimalCommand(null, codigo, nombre, sexo, fechaNacimiento, fechaNacimientoEstimada,
                    lote.razaPrincipalId(), categoriaActualId, color, lote.proposito(), OrigenAnimal.COMPRADO,
                    lote.propiedadActualId(), lote.potreroActualId(), null, lote.fechaIngreso(),
                    lote.precioAdquisicion(), null, condicionCorporalActual, null, observaciones, 0L,
                    pesoIngresoKg, pesoIngresoEstimado, false, false);
        }
    }

    public List<AnimalCommand> commands() {
        return animales.stream().map(item -> item.command(this)).toList();
    }
}
