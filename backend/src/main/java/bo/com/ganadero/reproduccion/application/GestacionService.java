package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.reproduccion.domain.*;
import bo.com.ganadero.reproduccion.infrastructure.GestacionStore;
import bo.com.ganadero.shared.error.*;
import bo.com.ganadero.shared.security.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class GestacionService {
 private static final ZoneId ZONE=ZoneId.of("America/La_Paz");
 private final GestacionStore store;
 private final ReproduccionRepository registros;
 private final AnimalRepository animales;
 private final UserContext context;
 private final ApplicationEventPublisher events;
 public GestacionService(GestacionStore store,ReproduccionRepository registros,AnimalRepository animales,UserContext context,ApplicationEventPublisher events){
  this.store=store;this.registros=registros;this.animales=animales;this.context=context;this.events=events;
 }
 private BusinessException error(String message){return new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,message);}
 @Transactional(readOnly=true) public List<GestacionCiclo> list(UUID animal){CurrentUser u=context.requirePermission("REPRODUCCION_VER");animal(u,animal,false);return store.list(u.empresaId(),animal);}
 private Animal animal(CurrentUser u,UUID id,boolean active){Animal a=animales.findById(id,u.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
  context.requirePropertyAccess(u,a.propiedadActualId());if(a.sexo()!=SexoAnimal.HEMBRA)throw new BusinessException(ErrorCode.REPRODUCCION_SOLO_HEMBRA);
  if(active&&a.estado()!=EstadoAnimal.ACTIVO)throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);return a;}
 @Transactional public GestacionCiclo registrar(UUID animalId,UUID diagnosticoId,LocalDate fecha,LocalDate inicio,String observaciones){
  CurrentUser u=context.requirePermission("REPRODUCCION_REGISTRAR");Animal a=animal(u,animalId,true);
  if(diagnosticoId!=null){DiagnosticoGestacion d=registros.findDiagnosticoById(diagnosticoId,u.empresaId()).orElseThrow(()->error("No existe el diagnóstico seleccionado."));
   if(!d.animalId().equals(animalId)||d.resultado()!=ResultadoGestacion.POSITIVO||d.estado()!=EstadoRegistroReproduccion.ACTIVO)throw error("Selecciona un diagnóstico positivo vigente de esta hembra.");
   return confirmar(u,a,d);
  }
  if(observaciones==null||observaciones.isBlank())throw error("Describe los antecedentes desconocidos y cómo se confirmó la gestación.");
  return abrir(u,a,null,null,true,fecha,inicio,observaciones);
 }
 public GestacionCiclo confirmar(CurrentUser u,Animal a,DiagnosticoGestacion d){
  List<GestacionCiclo> anteriores=store.list(u.empresaId(),a.id());
  Optional<GestacionCiclo> relacionada=anteriores.stream().filter(g->d.id().equals(g.diagnosticoId())||(d.servicioId()!=null&&d.servicioId().equals(g.servicioId()))).findFirst();
  if(relacionada.isPresent()){
   if(!"ABIERTA".equals(relacionada.get().estado()))throw error("Esta gestación ya está finalizada; no se puede reutilizar su diagnóstico o servicio.");
   store.linkDiagnostico(d.id(),relacionada.get().id());return relacionada.get();
  }
  LocalDate fecha=d.fechaDiagnostico().atZone(ZONE).toLocalDate();
  Optional<GestacionCiclo> abierta=anteriores.stream().filter(g->"ABIERTA".equals(g.estado())).findFirst();
  if(abierta.isPresent()){
   if(!Objects.equals(abierta.get().servicioId(),d.servicioId())||fecha.isBefore(abierta.get().fechaConfirmacion()))throw error("La hembra ya tiene otra gestación abierta. Revisa el servicio y las fechas.");
   store.linkDiagnostico(d.id(),abierta.get().id());return abierta.get();
  }
  LocalDate inicio=d.diasGestacionEstimados()==null?null:fecha.minusDays(d.diasGestacionEstimados());
  if(d.servicioId()!=null){Servicio s=registros.findServicioById(d.servicioId(),u.empresaId()).orElseThrow(()->error("Servicio no encontrado."));
   if(s.estado()==EstadoServicio.FINALIZADO||s.estado()==EstadoServicio.ANULADO||!s.hembraId().equals(a.id()))throw error("El servicio no está disponible para iniciar una gestación.");
   if(!d.fechaDiagnostico().isAfter(s.fechaServicio()))throw error("El diagnóstico debe ser posterior al servicio.");
   inicio=s.fechaServicio().atZone(ZONE).toLocalDate();
  }
  return abrir(u,a,d.servicioId(),d.id(),d.servicioId()==null,fecha,inicio,d.observaciones());
 }
 private GestacionCiclo abrir(CurrentUser u,Animal a,UUID servicio,UUID diagnostico,boolean desconocidos,LocalDate fecha,LocalDate inicio,String obs){
  if(fecha==null||fecha.isAfter(LocalDate.now(ZONE)))throw error("La fecha de confirmación es obligatoria y no puede ser futura.");
  if(inicio!=null&&inicio.isAfter(fecha))throw error("El inicio estimado no puede ser posterior a la confirmación.");
  LocalDate base=inicio==null?fecha:inicio;
  if(a.fechaNacimiento()!=null&&!base.isAfter(a.fechaNacimiento()))throw error("La gestación debe comenzar después del nacimiento de la madre.");
  if(store.list(u.empresaId(),a.id()).stream().anyMatch(g->"ABIERTA".equals(g.estado())))throw error("Esta hembra ya tiene una gestación abierta.");
  if(store.lastEnd(u.empresaId(),a.id()).filter(fin->!base.isAfter(fin)).isPresent())throw error("La nueva gestación debe comenzar después del último parto o aborto registrado. Revisa los antecedentes históricos.");
  GestacionCiclo g=new GestacionCiclo(UUID.randomUUID(),a.id(),servicio,diagnostico,desconocidos,fecha,inicio,obs,"ABIERTA",null,null);
  try{store.create(u.empresaId(),u.userId(),g);}catch(DataIntegrityViolationException ex){throw error("Ya existe una gestación para esta hembra, servicio o diagnóstico. Actualiza la pantalla.");}
  if(diagnostico!=null)store.linkDiagnostico(diagnostico,g.id());
  audit(u,"ABRIR_GESTACION",g.id());return g;
 }
 public void validarServicio(CurrentUser u,Animal a,Instant fecha){
  if(store.list(u.empresaId(),a.id()).stream().anyMatch(g->"ABIERTA".equals(g.estado())))throw error("La hembra tiene una gestación abierta; no admite un nuevo servicio.");
  if(store.lastEnd(u.empresaId(),a.id()).filter(fin->!fecha.atZone(ZONE).toLocalDate().isAfter(fin)).isPresent())throw error("El nuevo servicio debe ser posterior al último parto o aborto.");
 }
 public void validarDiagnostico(CurrentUser u,Animal a,UUID servicio,Instant fecha,ResultadoGestacion resultado){
  if(store.lastEnd(u.empresaId(),a.id()).filter(fin->!fecha.atZone(ZONE).toLocalDate().isAfter(fin)).isPresent())throw error("El diagnóstico de una nueva gestación debe ser posterior al último parto o aborto.");
  Optional<GestacionCiclo> abierta=store.list(u.empresaId(),a.id()).stream().filter(g->"ABIERTA".equals(g.estado())).findFirst();
  if(abierta.isPresent()){
   if(!Objects.equals(servicio,abierta.get().servicioId()))throw error("El diagnóstico debe corresponder al servicio de la gestación abierta.");
   if(fecha.atZone(ZONE).toLocalDate().isBefore(abierta.get().fechaConfirmacion()))throw error("El diagnóstico no puede ser anterior a la confirmación de la gestación.");
   if(resultado!=ResultadoGestacion.POSITIVO&&resultado!=ResultadoGestacion.DUDOSO)throw error("La gestación está abierta. Registra su aborto para finalizarla; un diagnóstico no debe borrar su historia.");
  }
  if(resultado==ResultadoGestacion.PERDIDA_GESTACION)throw error("Registra la pérdida desde Abortos, seleccionando la gestación correspondiente.");
 }
 public GestacionCiclo cerrar(CurrentUser u,UUID animal,UUID id,LocalDate fecha,boolean parto,UUID evento){
  if(id==null)throw error("Selecciona la gestación que finaliza. Si faltan antecedentes, regístrala explícitamente primero.");
  GestacionCiclo g=store.find(u.empresaId(),id).orElseThrow(()->error("Gestación no encontrada."));
  if(!g.animalId().equals(animal))throw error("La gestación no pertenece a esta hembra.");
  if(!"ABIERTA".equals(g.estado()))throw error("La gestación ya finalizó por parto o aborto y no admite otro desenlace.");
  if(fecha.isAfter(LocalDate.now(ZONE))||fecha.isBefore(g.fechaConfirmacion())||(g.fechaInicioEstimada()!=null&&!fecha.isAfter(g.fechaInicioEstimada())))throw error("La fecha del desenlace debe ser posterior al inicio y no anterior a la confirmación de la gestación.");
  Set<UUID> diagnosticos=store.diagnosticos(g.id());
  LocalDate ultimoDiagnostico=registros.diagnosticosDeAnimal(animal,u.empresaId()).stream()
   .filter(d->d.estado()==EstadoRegistroReproduccion.ACTIVO&&diagnosticos.contains(d.id()))
   .map(d->d.fechaDiagnostico().atZone(ZONE).toLocalDate()).max(LocalDate::compareTo).orElse(g.fechaConfirmacion());
  if(fecha.isBefore(ultimoDiagnostico))throw error("El desenlace no puede ser anterior al último diagnóstico de esta gestación.");
  if(!store.close(u.empresaId(),u.userId(),id,parto?"FINALIZADA_PARTO":"FINALIZADA_ABORTO",fecha,evento))throw error("La gestación ya fue finalizada por otra solicitud. Actualiza la pantalla.");
  audit(u,parto?"FINALIZAR_GESTACION_PARTO":"FINALIZAR_GESTACION_ABORTO",id);return g;
 }
 public void vincular(boolean parto,UUID evento,UUID ciclo){store.link(parto,evento,ciclo);}
 public Set<UUID> diagnosticos(UUID ciclo){return store.diagnosticos(ciclo);}
 public boolean asociarDiagnostico(CurrentUser u,DiagnosticoGestacion d){
  Optional<GestacionCiclo> abierta=store.list(u.empresaId(),d.animalId()).stream().filter(g->"ABIERTA".equals(g.estado())).findFirst();
  if(abierta.isEmpty())return false;
  store.linkDiagnostico(d.id(),abierta.get().id());return true;
 }
 private void audit(CurrentUser u,String accion,UUID id){events.publishEvent(new ReproduccionAuditEvent(u.empresaId(),u.userId(),accion,"GESTACION_CICLO",id,Instant.now()));}
}
