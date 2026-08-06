package bo.com.ganadero.animales.domain; import java.util.*;
public interface CategoriaAnimalRepository {List<CategoriaAnimal> findActive(UUID empresa);Optional<CategoriaAnimal> findById(UUID id,UUID empresa);}
