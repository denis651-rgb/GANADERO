package bo.com.ganadero.sanidad.application;
import bo.com.ganadero.sanidad.domain.SeveridadCaso;import java.time.Instant;import java.util.UUID;
public record CrearCasoClinicoCommand(UUID animalId,Instant fechaInicio,String sintomas,UUID enfermedadId,String diagnosticoTexto,SeveridadCaso severidad,UUID veterinarioId,String observaciones){}
