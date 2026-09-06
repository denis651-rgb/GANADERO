/**
 * Módulo proveedores del monolito modular GANADERO.
 *
 * <p>Entidad formal de proveedor: antes el dato vivía como texto libre en las observaciones del
 * animal. Las compras se relacionan con un proveedor de este módulo en vez de duplicar el dato.</p>
 *
 * <p>Las dependencias entre módulos deben realizarse mediante APIs públicas
 * de aplicación o eventos de dominio; nunca accediendo al repositorio o a la
 * infraestructura interna de otro módulo.</p>
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"shared"}
)
package bo.com.ganadero.proveedores;
