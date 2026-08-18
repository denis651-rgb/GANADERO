package bo.com.ganadero.sanidad.domain; import java.util.*;
public interface JornadaSanitariaRepository {
 JornadaSanitaria crear(JornadaSanitaria j,UUID actor); Optional<JornadaSanitaria> buscar(UUID id,UUID empresa);
 Optional<JornadaSanitaria> buscarPorOperacion(UUID operationId,UUID empresa); List<JornadaSanitaria> listar(UUID empresa);
 void reemplazarSeleccion(UUID jornada,UUID empresa,Collection<UUID> animales); List<UUID> seleccion(UUID jornada,UUID empresa);
 JornadaSanitaria iniciarConfirmacion(UUID id,UUID empresa,long version,UUID operationId,UUID actor);
 JornadaSanitaria confirmar(UUID id,UUID empresa,UUID actor); AplicacionSanitaria crearAplicacion(AplicacionSanitaria a,UUID actor);
 List<AplicacionSanitaria> aplicaciones(UUID jornada,UUID empresa);
 List<UUID> aplicacionesPrevias(UUID empresa,UUID animal,UUID planItem,UUID excluirAplicacion);
}
