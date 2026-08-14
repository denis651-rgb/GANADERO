package bo.com.ganadero.sanidad.application; import bo.com.ganadero.sanidad.domain.TipoActividadSanitaria;import java.time.LocalDate;import java.util.UUID;
public record CrearJornadaCommand(TipoActividadSanitaria tipoJornada,LocalDate fechaInicio,UUID propiedadId,UUID potreroId,UUID loteGanaderoId,UUID responsableId,UUID veterinarioId,String observaciones){}
