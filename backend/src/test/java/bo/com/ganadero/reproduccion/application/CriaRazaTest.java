package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.reproduccion.domain.*;
import bo.com.ganadero.shared.security.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.pesajes.domain.PesajeRepository;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class CriaRazaTest {
 @Test void guardaRazaElegidaYCategoriaDeCriaYRechazaRazaAusente() {
  var repo=mock(ReproduccionRepository.class);
  var animales=mock(AnimalRepository.class);
  var context=mock(UserContext.class);
  var user=mock(CurrentUser.class);
  var madre=mock(Animal.class);
  var razas=mock(RazaRepository.class);
  var categorias=mock(CategoriaAnimalRepository.class);
  UUID empresa=UUID.randomUUID(), madreId=UUID.randomUUID(), razaId=UUID.randomUUID(), categoriaId=UUID.randomUUID();
  when(user.empresaId()).thenReturn(empresa);
  when(context.requirePermission("REPRODUCCION_REGISTRAR")).thenReturn(user);
  when(animales.findById(madreId,empresa)).thenReturn(Optional.of(madre));
  when(madre.id()).thenReturn(madreId);
  when(madre.sexo()).thenReturn(SexoAnimal.HEMBRA);
  when(madre.estado()).thenReturn(EstadoAnimal.ACTIVO);
  when(repo.createParto(any(),any())).thenAnswer(i->i.getArgument(0));
  when(repo.createCria(any(),any())).thenAnswer(i->i.getArgument(0));
  when(razas.findById(razaId,empresa)).thenReturn(Optional.of(new Raza(razaId,null,"NELORE","Nelore","BOVINO",null,true)));
  when(categorias.findActive(empresa)).thenReturn(List.of(new CategoriaAnimal(categoriaId,null,"TERNERA","Ternera","HEMBRA",0,12,null,true)));
  var gestaciones=mock(GestacionService.class);
  when(gestaciones.cerrar(any(),any(),any(),any(),eq(true),any())).thenReturn(new GestacionCiclo(UUID.randomUUID(),madreId,null,null,true,LocalDate.now().minusDays(2),null,null,"ABIERTA",null,null));
  var service=new ReproduccionCicloService(repo,animales,mock(ParentescoRepository.class),mock(PesajeRepository.class),context,
   mock(TimelineEventPublisher.class),mock(ApplicationEventPublisher.class),mock(ObjectProvider.class),mock(CodigoService.class),razas,categorias,gestaciones,
   mock(ObjectProvider.class));
  service.registrarParto(command(madreId,razaId));
  verify(animales).create(argThat(a->razaId.equals(a.razaPrincipalId())&&categoriaId.equals(a.categoriaActualId())),any());
  assertThrows(BusinessException.class,()->service.registrarParto(command(madreId,null)));
  verify(animales,times(1)).create(any(),any());
 }
 private RegistrarPartoCommand command(UUID madre,UUID raza){
  return new RegistrarPartoCommand(madre,null,null,LocalDate.now().minusDays(1),TipoParto.NORMAL,DificultadParto.SIN_ASISTENCIA,false,null,null,null,
   List.of(new RegistrarPartoCommand.CriaCommand(SexoAnimal.HEMBRA,null,EstadoNacimiento.VIVO,null,null,true,null,"Luna",null,raza)),UUID.randomUUID());
 }
}
