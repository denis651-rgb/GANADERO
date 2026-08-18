package bo.com.ganadero.alertas.application;
import bo.com.ganadero.alertas.domain.SeveridadAlerta;
import java.time.Instant;
import java.util.UUID;
public record CrearRecordatorioCommand(String titulo,String mensaje,SeveridadAlerta severidad,UUID animalId,
        Instant fechaEvento,Instant primeraNotificacion,int cantidadNotificaciones,Integer intervaloMinutos) {}
