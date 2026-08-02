package bo.com.ganadero.animales.domain; import java.util.UUID;
public record Raza(UUID id,UUID empresaId,String codigo,String nombre,String especie,String descripcion,boolean activo) {}
