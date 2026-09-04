package bo.com.ganadero.sanidad.application;
import bo.com.ganadero.sanidad.domain.EstadoCalostrado;import bo.com.ganadero.sanidad.domain.MomentoControlNeonatal;import java.math.BigDecimal;import java.time.LocalDate;import java.util.UUID;
public record CrearControlNeonatalCommand(UUID animalId,LocalDate fechaControl,MomentoControlNeonatal momento,EstadoCalostrado calostrado,boolean ombligoDesinfectado,String ombligoEstado,boolean diarrea,String estadoGeneral,String lactancia,BigDecimal temperaturaC,String observaciones){}
