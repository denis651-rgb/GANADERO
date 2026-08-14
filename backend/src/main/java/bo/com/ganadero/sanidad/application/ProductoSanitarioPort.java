package bo.com.ganadero.sanidad.application; import java.util.UUID;
/** Puerto hacia Inventario. Sanidad nunca consulta directamente sus tablas internas. */
public interface ProductoSanitarioPort {
 boolean existeYEstaActivo(UUID empresaId,UUID productoId);
 default ProductoSanitarioInfo informacion(UUID empresaId,UUID productoId){return new ProductoSanitarioInfo(productoId,0,0);}
 record ProductoSanitarioInfo(UUID id,int retiroCarneDias,int retiroLecheDias) {}
}
