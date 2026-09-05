package bo.com.ganadero.animales.domain; import java.util.List; import java.util.UUID;
public interface HistorialCategoriaAnimalRepository {
    void crear(HistorialCategoriaAnimal historial);
    List<HistorialCategoriaAnimal> listar(UUID animalId);
    boolean existeParaCategoria(UUID categoriaId);
}
