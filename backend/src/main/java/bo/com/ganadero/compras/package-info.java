/**
 * Módulo compras del monolito modular GANADERO.
 *
 * <p>Toda incorporación de animales comprados (individual o por lote) pertenece a una Compra
 * formal: encabezado + detalle por animal + proveedor. Reutiliza animales (AnimalService.create
 * por detalle), movimientos (INGRESO_COMPRA) y pesajes (evento de peso motivo COMPRA) en vez de
 * reimplementar esa lógica — la orquestación completa vive en una única transacción.</p>
 *
 * <p>Las dependencias entre módulos deben realizarse mediante APIs públicas
 * de aplicación o eventos de dominio; nunca accediendo al repositorio o a la
 * infraestructura interna de otro módulo.</p>
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"animales", "movimientos", "pesajes", "proveedores", "shared"}
)
package bo.com.ganadero.compras;
