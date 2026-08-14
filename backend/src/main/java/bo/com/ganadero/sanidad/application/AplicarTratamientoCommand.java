package bo.com.ganadero.sanidad.application;
import java.math.BigDecimal;
public record AplicarTratamientoCommand(BigDecimal dosisAplicada,String observaciones,long version){}
