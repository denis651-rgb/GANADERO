package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.reproduccion.domain.*;
import bo.com.ganadero.reproduccion.infrastructure.GestacionStore;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.security.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.sqlite.*;
import java.nio.file.Path;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestacionServiceTest {
 @TempDir Path dir;
 GestacionService service;GestacionStore store;JdbcClient jdbc;TransactionTemplate tx;
 final UUID animalId=UUID.randomUUID(), empresa=UUID.randomUUID(), actor=UUID.randomUUID();
 final CurrentUser user=new CurrentUser(actor,empresa,actor,Set.of(),Set.of(),Set.of(),true);
 final LocalDate fecha=LocalDate.now().minusDays(30);
 ReproduccionRepository registros;Animal animal;
 @BeforeEach void setup(){
  var ds=new SQLiteDataSource();ds.setUrl("jdbc:sqlite:"+dir.resolve("test.db"));
  Flyway.configure().dataSource(ds).locations("classpath:db/migration").load().migrate();
  var config=new SQLiteConfig();config.enforceForeignKeys(true);
  var db=new SQLiteDataSource(config);db.setUrl(ds.getUrl());jdbc=JdbcClient.create(db);tx=new TransactionTemplate(new DataSourceTransactionManager(db));
  UUID potrero=UUID.randomUUID();
  jdbc.sql("insert into potrero(id,codigo,nombre,propiedad_id) values(?,'P','Corral','00000000-0000-0000-0000-000000000001')").param(potrero.toString()).update();
  jdbc.sql("""
   insert into animal(id,codigo,nombre,sexo,raza_principal_id,categoria_actual_id,proposito,origen,potrero_actual_id,fecha_ingreso,propiedad_actual_id)
   values(?,'A','Vaca','HEMBRA','50000000-0000-0000-0000-000000000001','60000000-0000-0000-0000-000000000005','CARNE','COMPRADO',?,'2020-01-01','00000000-0000-0000-0000-000000000001')
   """).params(animalId.toString(),potrero.toString()).update();
  registros=mock(ReproduccionRepository.class);var animales=mock(AnimalRepository.class);animal=mock(Animal.class);
  when(animal.id()).thenReturn(animalId);when(animal.estado()).thenReturn(EstadoAnimal.ACTIVO);when(animal.sexo()).thenReturn(SexoAnimal.HEMBRA);
  when(animal.fechaNacimiento()).thenReturn(LocalDate.of(2020,1,1));when(animales.findById(animalId,empresa)).thenReturn(Optional.of(animal));
  store=new GestacionStore(jdbc);service=new GestacionService(store,registros,animales,new UserContext(()->user),mock(ApplicationEventPublisher.class));
 }
 GestacionCiclo abrir(LocalDate f){return tx.execute(s->service.registrar(animalId,null,f,null,"Comprada gestante; antecedentes desconocidos, revisión registrada."));}
 @Test void partoBloqueaAbortoYPermiteOtraGestacionPosterior(){
  var g=abrir(fecha);UUID parto=UUID.randomUUID();
  tx.executeWithoutResult(s->service.cerrar(user,animalId,g.id(),fecha.plusDays(10),true,parto));
  assertEquals("FINALIZADA_PARTO",service.list(animalId).getFirst().estado());
  assertThrows(BusinessException.class,()->tx.executeWithoutResult(s->service.cerrar(user,animalId,g.id(),fecha.plusDays(11),false,UUID.randomUUID())));
  assertThrows(BusinessException.class,()->abrir(fecha.plusDays(10)));
  var nueva=abrir(fecha.plusDays(11));assertNotEquals(g.id(),nueva.id());
  tx.executeWithoutResult(s->service.cerrar(user,animalId,nueva.id(),fecha.plusDays(20),false,UUID.randomUUID()));
  assertEquals("FINALIZADA_ABORTO",service.list(animalId).getFirst().estado());
  assertThrows(BusinessException.class,()->service.cerrar(user,animalId,nueva.id(),fecha.plusDays(21),true,UUID.randomUUID()));
 }
 @Test void rechazaDuplicadosFechasInvalidasYHembraIncorrecta(){
  assertThrows(BusinessException.class,()->service.registrar(animalId,null,fecha,fecha.plusDays(1),"Información"));
  assertThrows(BusinessException.class,()->abrir(LocalDate.now().plusDays(1)));
  var g=abrir(fecha);assertThrows(BusinessException.class,()->abrir(fecha.plusDays(1)));
  assertThrows(BusinessException.class,()->service.cerrar(user,animalId,g.id(),fecha.minusDays(1),true,UUID.randomUUID()));
  assertThrows(BusinessException.class,()->service.cerrar(user,UUID.randomUUID(),g.id(),fecha,true,UUID.randomUUID()));
  assertThrows(BusinessException.class,()->service.cerrar(user,animalId,null,fecha,true,UUID.randomUUID()));
  assertThrows(BusinessException.class,()->service.validarServicio(user,animal,Instant.now()));
 }
 @Test void rollbackConservaAbiertaSiFallaElRegistroDelDesenlace(){
  var g=abrir(fecha);
  assertThrows(IllegalStateException.class,()->tx.executeWithoutResult(s->{service.cerrar(user,animalId,g.id(),fecha,true,UUID.randomUUID());throw new IllegalStateException("falló la creación de la cría");}));
  assertEquals("ABIERTA",service.list(animalId).getFirst().estado());
 }
 @Test void cierreAtomicoYEstadoInmutable(){
  var g=abrir(fecha);
  assertTrue(store.close(empresa,actor,g.id(),"FINALIZADA_PARTO",fecha,UUID.randomUUID()));
  assertFalse(store.close(empresa,actor,g.id(),"FINALIZADA_ABORTO",fecha,UUID.randomUUID()));
  assertThrows(org.springframework.dao.DataAccessException.class,()->jdbc.sql("update gestacion_ciclo set estado='ABIERTA',fecha_cierre=null,evento_id=null where id=?").param(g.id().toString()).update());
 }
 @Test void noAceptaDiagnosticoDeOtraHembraNiCreaServicioFicticio(){
  UUID id=UUID.randomUUID();var d=mock(DiagnosticoGestacion.class);when(d.animalId()).thenReturn(UUID.randomUUID());
  when(registros.findDiagnosticoById(id,empresa)).thenReturn(Optional.of(d));
  assertThrows(BusinessException.class,()->service.registrar(animalId,id,null,null,null));
  var g=abrir(fecha);assertNull(g.servicioId());assertTrue(g.antecedentesDesconocidos());
  verify(registros,never()).createServicio(any(),any());
 }
 @Test void diagnosticosPositivosCompartenCicloYNoPermitenCierreAnteriorAlUltimo(){
  var primero=diagnostico(fecha);
  GestacionCiclo g=tx.execute(s->service.confirmar(user,animal,primero));
  var segundo=diagnostico(fecha.plusDays(5));
  assertEquals(g.id(),tx.execute(s->service.confirmar(user,animal,segundo)).id());
  assertEquals(1,service.list(animalId).size());
  when(registros.diagnosticosDeAnimal(animalId,empresa)).thenReturn(List.of(primero,segundo));
  assertThrows(BusinessException.class,()->service.cerrar(user,animalId,g.id(),fecha.plusDays(2),true,UUID.randomUUID()));
  tx.executeWithoutResult(s->service.cerrar(user,animalId,g.id(),fecha.plusDays(6),true,UUID.randomUUID()));
  assertThrows(BusinessException.class,()->service.confirmar(user,animal,primero));
  assertThrows(BusinessException.class,()->service.confirmar(user,animal,segundo));
 }
 private DiagnosticoGestacion diagnostico(LocalDate f){
  UUID id=UUID.randomUUID();Instant instante=f.atTime(12,0).atZone(ZoneId.of("America/La_Paz")).toInstant();
  jdbc.sql("insert into diagnostico_gestacion(id,animal_id,fecha_diagnostico,resultado) values(?,?,?,'POSITIVO')").params(id.toString(),animalId.toString(),instante.toString()).update();
  return new DiagnosticoGestacion(id,empresa,animalId,null,instante,ResultadoGestacion.POSITIVO,MetodoDiagnostico.OTRO,null,null,null,"Confirmación",null,null,null,null,null,EstadoRegistroReproduccion.ACTIVO,null,null,null,null,0);
 }
}
