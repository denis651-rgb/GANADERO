package bo.com.ganadero.sanidad.api;
import bo.com.ganadero.sanidad.application.AplicarTratamientoCommand;import jakarta.validation.constraints.*;import java.math.BigDecimal;
public record AplicarTratamientoRequest(@NotNull @DecimalMin("0.001") BigDecimal dosisAplicada,String observaciones,@Min(0) long version){public AplicarTratamientoCommand command(){return new AplicarTratamientoCommand(dosisAplicada,observaciones,version);}}
