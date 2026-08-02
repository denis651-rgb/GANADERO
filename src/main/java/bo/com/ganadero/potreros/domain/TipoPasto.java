package bo.com.ganadero.potreros.domain; import java.util.UUID;
public record TipoPasto(UUID id,UUID empresaId,String codigo,String nombre,String nombreCientifico,String descripcion,boolean activo) {}
