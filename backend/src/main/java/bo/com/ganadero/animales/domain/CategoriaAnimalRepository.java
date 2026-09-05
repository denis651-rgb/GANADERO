package bo.com.ganadero.animales.domain; import java.util.*;
public interface CategoriaAnimalRepository {
    List<CategoriaAnimal> findActive(UUID empresa);
    Optional<CategoriaAnimal> findById(UUID id, UUID empresa);
    List<CategoriaAnimal> findAllIncludingInactive();
    CategoriaAnimal crear(CategoriaAnimal categoria);
    CategoriaAnimal actualizar(CategoriaAnimal categoria);
    void cambiarEstado(UUID id, boolean activo);
    void eliminar(UUID id);
}
