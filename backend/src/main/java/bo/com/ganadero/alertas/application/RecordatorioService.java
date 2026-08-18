package bo.com.ganadero.alertas.application;

import bo.com.ganadero.alertas.domain.*;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class RecordatorioService {
    private final RecordatorioRepository repository; private final MotorAlertas motor;
    private final UserContext context; private final AnimalRepository animales;
    public RecordatorioService(RecordatorioRepository repository,MotorAlertas motor,UserContext context,AnimalRepository animales){
        this.repository=repository;this.motor=motor;this.context=context;this.animales=animales;
    }
    @Transactional public Recordatorio crear(CrearRecordatorioCommand c){
        CurrentUser u=context.requirePermission("ALERTA_CONFIGURAR"); validar(c);
        if(c.animalId()!=null){var a=animales.findById(c.animalId(),u.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));context.requirePropertyAccess(u,a.propiedadActualId());}
        UUID id=UUID.randomUUID();return repository.guardar(new Recordatorio(id,u.empresaId(),u.userId(),c.titulo().trim(),c.mensaje().trim(),c.severidad(),c.animalId(),c.fechaEvento(),c.primeraNotificacion(),c.cantidadNotificaciones(),c.intervaloMinutos(),0,EstadoRecordatorio.ACTIVO,null,null,0));
    }
    @Transactional(readOnly=true) public List<Recordatorio> listar(){CurrentUser u=context.requirePermission("ALERTA_VER");return repository.listar(u.empresaId());}
    @Transactional public Recordatorio pausar(UUID id,long v){return estado(id,v,EstadoRecordatorio.PAUSADO);}
    @Transactional public Recordatorio reanudar(UUID id,long v){return estado(id,v,EstadoRecordatorio.ACTIVO);}
    @Transactional public Recordatorio cancelar(UUID id,long v){CurrentUser u=context.requirePermission("ALERTA_CONFIGURAR");Recordatorio r=repository.cambiarEstado(id,u.empresaId(),EstadoRecordatorio.CANCELADO,v);repository.cancelarAlertas(id,u.empresaId());return r;}
    private Recordatorio estado(UUID id,long v,EstadoRecordatorio e){CurrentUser u=context.requirePermission("ALERTA_CONFIGURAR");Recordatorio actual=repository.buscar(id,u.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.ALERTA_NOT_FOUND));if(actual.estado()==EstadoRecordatorio.COMPLETADO||actual.estado()==EstadoRecordatorio.CANCELADO)throw new BusinessException(ErrorCode.ALERTA_NOT_FOUND);return repository.cambiarEstado(id,u.empresaId(),e,v);}
    @Transactional public int procesar(){Instant now=Instant.now();List<Recordatorio> due=repository.bloquearVencidos(now,100);for(Recordatorio r:due){int ocurrencia=r.notificacionesGeneradas()+1;UUID origen=UUID.nameUUIDFromBytes((r.id()+":"+ocurrencia).getBytes(StandardCharsets.UTF_8));Map<String,Object> md=new HashMap<>();md.put("recordatorioId",r.id().toString());md.put("ocurrencia",ocurrencia);md.put("tituloPersonalizado",r.titulo());md.put("mensajePersonalizado",r.mensaje());md.put("severidad",r.severidad().name());md.put("fechaEvento",r.fechaEvento().toString());md.put("eventoReferencia",r.id()+":"+ocurrencia);motor.crearInmediata(new ProgramarAlertaCommand(r.empresaId(),r.animalId(),TipoAlerta.RECORDATORIO_SANIDAD,now,"RECORDATORIO_OCURRENCIA",origen,md));boolean done=ocurrencia>=r.cantidadNotificaciones();Instant next=done?null:r.proximaEjecucion().plus(Duration.ofMinutes(r.intervaloMinutos()));repository.registrarEjecucion(r,next,done);}return due.size();}
    private void validar(CrearRecordatorioCommand c){if(c.titulo()==null||c.titulo().isBlank()||c.mensaje()==null||c.mensaje().isBlank()||c.severidad()==null||c.fechaEvento()==null||c.primeraNotificacion()==null)invalid("Datos incompletos");if(c.cantidadNotificaciones()<1||c.cantidadNotificaciones()>10)invalid("La cantidad debe estar entre 1 y 10");if(c.primeraNotificacion().isAfter(c.fechaEvento()))invalid("El primer aviso debe ser anterior al evento");if(c.cantidadNotificaciones()>1&&(c.intervaloMinutos()==null||c.intervaloMinutos()<15))invalid("El intervalo mínimo es 15 minutos");Instant last=c.cantidadNotificaciones()==1?c.primeraNotificacion():c.primeraNotificacion().plus(Duration.ofMinutes((long)c.intervaloMinutos()*(c.cantidadNotificaciones()-1)));if(last.isAfter(c.fechaEvento()))invalid("Las notificaciones deben ocurrir antes del evento");}
    private void invalid(String message){throw new BusinessException(ErrorCode.VALIDATION_ERROR,message);}
}
