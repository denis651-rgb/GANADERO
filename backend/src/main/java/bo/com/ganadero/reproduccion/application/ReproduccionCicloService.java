package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.alertas.application.ProgramarAlertaCommand;
import bo.com.ganadero.alertas.application.TipoAlerta;
import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.pesajes.domain.*;
import bo.com.ganadero.reproduccion.domain.*;
import bo.com.ganadero.shared.audit.AuditActions;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.*;
import bo.com.ganadero.shared.security.*;
import bo.com.ganadero.timeline.application.*;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;

@Service
public class ReproduccionCicloService {
 private final ReproduccionRepository repo; private final AnimalRepository animales; private final ParentescoRepository parentescos;
 private final PesajeRepository pesajes; private final UserContext context; private final TimelineEventPublisher timeline;
 private final ApplicationEventPublisher events; private final ObjectProvider<MotorAlertas> alertas;
 private final CodigoService codigos;
 private int diasHastaDestete=210; private int diasAlertaDestete=7;
 public ReproduccionCicloService(ReproduccionRepository repo,AnimalRepository animales,ParentescoRepository parentescos,
  PesajeRepository pesajes,UserContext context,TimelineEventPublisher timeline,ApplicationEventPublisher events,
  ObjectProvider<MotorAlertas> alertas,CodigoService codigos){this.repo=repo;this.animales=animales;this.parentescos=parentescos;this.pesajes=pesajes;
  this.context=context;this.timeline=timeline;this.events=events;this.alertas=alertas;this.codigos=codigos;}
 @Value("${ganadero.reproduccion.dias-hasta-destete:210}") void setDiasHastaDestete(int dias){if(dias<1)throw new IllegalArgumentException();this.diasHastaDestete=dias;}
 @Value("${ganadero.reproduccion.dias-alerta-destete:7}") void setDiasAlertaDestete(int dias){if(dias<0)throw new IllegalArgumentException();this.diasAlertaDestete=dias;}

 @Transactional
 public PartoResult registrarParto(RegistrarPartoCommand c){
  CurrentUser u=context.requirePermission("REPRODUCCION_REGISTRAR"); Animal madre=hembra(u,c.madreId());
  LocalDate fecha=fecha(c.fechaParto()); validarPosteriorNacimiento(fecha,madre); if(c.crias()==null||c.crias().isEmpty()) throw new BusinessException(ErrorCode.PARTO_CRIAS_INVALIDAS);
  DiagnosticoGestacion dg=null; if(c.diagnosticoGestacionId()!=null){dg=repo.findDiagnosticoById(c.diagnosticoGestacionId(),u.empresaId())
   .orElseThrow(()->new BusinessException(ErrorCode.PARTO_GESTACION_INCOMPATIBLE));
   if(!dg.animalId().equals(madre.id())||dg.resultado()!=ResultadoGestacion.POSITIVO) throw new BusinessException(ErrorCode.PARTO_GESTACION_INCOMPATIBLE);
   if(repo.existsActivePartoForGestacion(u.empresaId(),dg.id())) throw new BusinessException(ErrorCode.PARTO_GESTACION_DUPLICADA);}
  Servicio servicio=servicioCompatible(u,c.servicioId(),madre.id()); UUID partoId=UUID.randomUUID();
  Parto parto=repo.createParto(new Parto(partoId,u.empresaId(),madre.id(),c.diagnosticoGestacionId(),c.servicioId(),fecha,
   c.tipoParto()==null?TipoParto.NORMAL:c.tipoParto(),c.dificultad()==null?DificultadParto.SIN_ASISTENCIA:c.dificultad(),
   c.asistido(),c.responsableId(),c.resultadoMadre(),c.crias().size(),c.observaciones(),madre.propiedadActualId(),
   madre.potreroActualId(),madre.loteActualId(),partoId,null,EstadoRegistroReproduccion.ACTIVO,null,null,null,
   null,null,null,null,null,null,null,0),u.userId());
  List<CriaParto> creadas=new ArrayList<>(); for(RegistrarPartoCommand.CriaCommand item:c.crias()) creadas.add(crearCria(u,madre,servicio,parto,item));
  if(c.servicioId()!=null) repo.updateServicioEstado(c.servicioId(),u.empresaId(),EstadoServicio.FINALIZADO,u.userId());
  publicar(u,madre.id(),TipoEventoAnimal.PARTO_REGISTRADO,parto.id(),"Parto registrado");
  audit(u,AuditActions.REGISTRAR_PARTO,"PARTO",parto.id()); resolver(u,"GESTACION",c.diagnosticoGestacionId()); resolver(u,"MADRE",madre.id());
  return new PartoResult(parto,creadas);
 }

 @Transactional(readOnly=true) public PartoPage listarPartos(UUID animal,UUID propiedad,int page,int size){CurrentUser u=context.requirePermission("REPRODUCCION_VER");
  if(propiedad!=null)context.requirePropertyAccess(u,propiedad);return repo.findAllPartos(u.empresaId(),u.propiedadesPermitidas(),u.accesoTodasPropiedades(),animal,propiedad,page,size);}
 @Transactional(readOnly=true) public AbortoPage listarAbortos(UUID animal,UUID propiedad,int page,int size){CurrentUser u=context.requirePermission("REPRODUCCION_VER");
  if(propiedad!=null)context.requirePropertyAccess(u,propiedad);return repo.findAllAbortos(u.empresaId(),u.propiedadesPermitidas(),u.accesoTodasPropiedades(),animal,propiedad,page,size);}
 @Transactional(readOnly=true) public DestetePage listarDestetes(UUID animal,UUID propiedad,int page,int size){CurrentUser u=context.requirePermission("REPRODUCCION_VER");
  if(propiedad!=null)context.requirePropertyAccess(u,propiedad);return repo.findAllDestetes(u.empresaId(),u.propiedadesPermitidas(),u.accesoTodasPropiedades(),animal,propiedad,page,size);}

 private CriaParto crearCria(CurrentUser u,Animal madre,Servicio servicio,Parto parto,RegistrarPartoCommand.CriaCommand c){
  UUID animalId=null; if(c.crearAnimal()){
   if(c.estadoNacimiento()!=EstadoNacimiento.VIVO||c.nombreAnimal()==null||c.nombreAnimal().isBlank())
    throw new BusinessException(ErrorCode.CRIA_ANIMAL_DATOS_REQUERIDOS);
   animalId=UUID.randomUUID(); UUID potrero=c.potreroInicialId()==null?madre.potreroActualId():c.potreroInicialId();
   if(potrero!=null&&!animales.validLocation(u.empresaId(),madre.propiedadActualId(),potrero)) throw new BusinessException(ErrorCode.INVALID_ANIMAL_LOCATION);
   String codigo=codigos.paraCreacion(u,TipoCodigo.ANIMAL,null,null,c.codigoAnimal());
   Animal animal=new Animal(animalId,u.empresaId(),codigo,c.nombreAnimal(),c.sexo(),parto.fechaParto(),false,null,null,null,
    madre.proposito(),OrigenAnimal.NACIDO,madre.propiedadActualId(),potrero,null,EstadoAnimal.ACTIVO,parto.fechaParto(),null,
    c.pesoNacimientoKg(),null,null,c.observaciones(),0); animales.create(animal,u.userId());
   parentescos.create(new Parentesco(UUID.randomUUID(),u.empresaId(),animalId,TipoParentesco.MADRE,madre.id(),null,null,null,Instant.now(),u.userId()),u.userId());
   if(servicio!=null&&servicio.tipoServicio()==TipoServicio.MONTA_NATURAL&&servicio.machoId()!=null)
    parentescos.create(new Parentesco(UUID.randomUUID(),u.empresaId(),animalId,TipoParentesco.PADRE,servicio.machoId(),null,null,null,Instant.now(),u.userId()),u.userId());
   if(c.pesoNacimientoKg()!=null) pesajes.create(new Pesaje(UUID.randomUUID(),u.empresaId(),animalId,parto.fechaParto(),c.pesoNacimientoKg(),
    TipoPesaje.NACIMIENTO,null,null,u.userId(),madre.propiedadActualId(),potrero,null,null,UUID.randomUUID(),null,EstadoPesaje.ACTIVO,
    null,null,null,"Peso registrado al nacer",null,null,null,null,null,null,0),u.userId());
   publicar(u,animalId,TipoEventoAnimal.CRIA_REGISTRADA,parto.id(),"Cría registrada");
   MotorAlertas motor=alertas.getIfAvailable(); if(motor!=null)motor.programar(new ProgramarAlertaCommand(u.empresaId(),animalId,
    TipoAlerta.DESTETE_PROXIMO,parto.fechaParto().plusDays(diasHastaDestete-diasAlertaDestete).atStartOfDay(ZoneOffset.UTC).toInstant(),
    parto.fechaParto().plusDays(diasHastaDestete).atStartOfDay(ZoneOffset.UTC).toInstant(),"CRIA",animalId,Map.of("madreId",madre.id(),"partoId",parto.id())));
  }
  UUID id=UUID.randomUUID(); CriaParto cria=repo.createCria(new CriaParto(id,u.empresaId(),parto.id(),animalId,c.sexo(),c.pesoNacimientoKg(),
   c.estadoNacimiento(),c.horaNacimiento(),c.observaciones(),id,null,null,null,0),u.userId()); audit(u,"REGISTRAR_CRIA","CRIA_PARTO",id); return cria;
 }

 @Transactional public Aborto registrarAborto(RegistrarAbortoCommand c){CurrentUser u=context.requirePermission("REPRODUCCION_REGISTRAR"); Animal a=hembra(u,c.animalId());
  LocalDate f=fecha(c.fechaEvento()); validarPosteriorNacimiento(f,a); if(c.gestacionId()!=null){DiagnosticoGestacion d=repo.findDiagnosticoById(c.gestacionId(),u.empresaId())
   .orElseThrow(()->new BusinessException(ErrorCode.PARTO_GESTACION_INCOMPATIBLE)); if(!d.animalId().equals(a.id()))throw new BusinessException(ErrorCode.PARTO_GESTACION_INCOMPATIBLE);
   repo.updateDiagnosticoResultado(d.id(),u.empresaId(),ResultadoGestacion.PERDIDA_GESTACION,u.userId());}
  servicioCompatible(u,c.servicioId(),a.id()); if(c.servicioId()!=null)repo.updateServicioEstado(c.servicioId(),u.empresaId(),EstadoServicio.FINALIZADO,u.userId());
  UUID id=UUID.randomUUID(); Aborto saved=repo.createAborto(new Aborto(id,u.empresaId(),a.id(),c.gestacionId(),c.servicioId(),f,c.edadGestacionalEstimada(),
   c.causa(),c.diagnostico(),c.veterinarioId(),c.observaciones(),a.propiedadActualId(),a.potreroActualId(),a.loteActualId(),id,null,
   EstadoRegistroReproduccion.ACTIVO,null,null,null,null,0),u.userId()); publicar(u,a.id(),TipoEventoAnimal.ABORTO_REGISTRADO,id,"Aborto registrado");
  audit(u,"REGISTRAR_ABORTO","ABORTO",id); cancelar(u,"GESTACION",c.gestacionId(),"GESTACION_PERDIDA"); cancelar(u,"MADRE",a.id(),"GESTACION_PERDIDA"); return saved;}

 @Transactional public Destete registrarDestete(RegistrarDesteteCommand c){CurrentUser u=context.requirePermission("REPRODUCCION_REGISTRAR");
  Animal cria=animal(u,c.animalCriaId()); Animal madre=hembra(u,c.madreId()); LocalDate f=fecha(c.fechaDestete());
  if(cria.fechaNacimiento()!=null&&!f.isAfter(cria.fechaNacimiento()))throw new BusinessException(ErrorCode.DESTETE_FECHA_INVALIDA);
  CriaParto cp=repo.findCriaByAnimal(cria.id(),u.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
  Parto p=repo.findPartoById(cp.partoId(),u.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
  if(!p.madreId().equals(madre.id()))throw new BusinessException(ErrorCode.PARTO_GESTACION_INCOMPATIBLE);
  UUID id=UUID.randomUUID(); Destete d=repo.createDestete(new Destete(id,u.empresaId(),cria.id(),madre.id(),f,c.pesoDesteteKg(),
   c.tipoDestete()==null?TipoDestete.NORMAL:c.tipoDestete(),c.motivo(),c.responsableId(),c.observaciones(),cria.propiedadActualId(),
   cria.potreroActualId(),cria.loteActualId(),id,null,EstadoRegistroReproduccion.ACTIVO,null,null,null,null,0),u.userId());
  if(c.pesoDesteteKg()!=null)pesajes.create(new Pesaje(UUID.randomUUID(),u.empresaId(),cria.id(),f,c.pesoDesteteKg(),TipoPesaje.DESTETE,null,null,
   c.responsableId()==null?u.userId():c.responsableId(),cria.propiedadActualId(),cria.potreroActualId(),cria.loteActualId(),null,UUID.randomUUID(),null,
   EstadoPesaje.ACTIVO,null,null,null,"Peso al destete",null,null,null,null,null,null,0),u.userId());
  publicar(u,cria.id(),TipoEventoAnimal.DESTETE_REGISTRADO,id,"Destete registrado"); audit(u,AuditActions.REGISTRAR_DESTETE,"DESTETE",id); resolver(u,"CRIA",cria.id()); return d;}

 private Servicio servicioCompatible(CurrentUser u,UUID id,UUID hembra){if(id==null)return null; Servicio s=repo.findServicioById(id,u.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
  if(!s.hembraId().equals(hembra)||s.estado()==EstadoServicio.ANULADO)throw new BusinessException(ErrorCode.DIAGNOSTICO_SERVICIO_INCOMPATIBLE);return s;}
 private Animal hembra(CurrentUser u,UUID id){Animal a=animal(u,id);if(a.sexo()!=SexoAnimal.HEMBRA)throw new BusinessException(ErrorCode.REPRODUCCION_SOLO_HEMBRA);return a;}
 private Animal animal(CurrentUser u,UUID id){Animal a=animales.findById(id,u.empresaId()).orElseThrow(()->new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));context.requirePropertyAccess(u,a.propiedadActualId());if(a.estado()!=EstadoAnimal.ACTIVO)throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);return a;}
 private LocalDate fecha(LocalDate f){LocalDate v=f==null?LocalDate.now():f;if(v.isAfter(LocalDate.now()))throw new BusinessException(ErrorCode.REPRODUCCION_FECHA_INVALIDA);return v;}
 private void validarPosteriorNacimiento(LocalDate fecha,Animal animal){if(animal.fechaNacimiento()!=null&&!fecha.isAfter(animal.fechaNacimiento()))throw new BusinessException(ErrorCode.REPRODUCCION_FECHA_ANTERIOR_NACIMIENTO);}
 private void resolver(CurrentUser u,String tipo,UUID id){if(id!=null){MotorAlertas m=alertas.getIfAvailable();if(m!=null)m.resolverPorOrigen(u.empresaId(),tipo,id);}}
 private void cancelar(CurrentUser u,String tipo,UUID id,String motivo){if(id!=null){MotorAlertas m=alertas.getIfAvailable();if(m!=null)m.cancelarPorOrigen(u.empresaId(),tipo,id,motivo);}}
 private void publicar(CurrentUser u,UUID animal,TipoEventoAnimal tipo,UUID origen,String texto){timeline.publish(new RegistrarEventoTimeline(u.empresaId(),animal,tipo,null,texto,null,origen,Map.of(),u.userId(),Instant.now(),null));}
 private void audit(CurrentUser u,String accion,String entidad,UUID id){events.publishEvent(new ReproduccionAuditEvent(u.empresaId(),u.userId(),accion,entidad,id,Instant.now()));}
}
