package bo.com.ganadero.sanidad.application;import bo.com.ganadero.sanidad.domain.*;import java.util.List;
public record ConfirmacionJornadaResult(JornadaSanitaria jornada,List<AplicacionSanitaria> aplicaciones,int totalProcesado){}
