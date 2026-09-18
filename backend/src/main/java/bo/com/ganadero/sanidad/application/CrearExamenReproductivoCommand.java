package bo.com.ganadero.sanidad.application;
import bo.com.ganadero.sanidad.domain.EnfermedadReproductiva;import bo.com.ganadero.sanidad.domain.ResultadoExamenReproductivo;import bo.com.ganadero.sanidad.domain.ResultadoPruebaReproductiva;import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;import java.util.UUID;
public record CrearExamenReproductivoCommand(UUID animalId,LocalDate fecha,ResultadoExamenReproductivo resultado,String veterinarioId,BigDecimal circunferenciaEscrotalCm,BigDecimal motilidadEspermaticaPct,BigDecimal morfologiaPct,String libido,String capacidadServicio,BigDecimal pesoKg,BigDecimal porcentajePesoAdulto,BigDecimal condicionCorporal,String desarrolloReproductivo,String observaciones,List<Prueba> pruebas){
 public record Prueba(EnfermedadReproductiva enfermedad,ResultadoPruebaReproductiva resultado){}
}
