package bo.com.ganadero.animales.domain; import java.util.UUID;
public record AnimalFilter(EstadoAnimal estado,UUID propiedadId,UUID potreroId,UUID loteId,String categoria,
 SexoAnimal sexo,String search,int page,int size) {}
