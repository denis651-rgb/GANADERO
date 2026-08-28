/**
 * Módulo ventas del monolito modular GANADERO.
 *
 * <p>Registra el precio y comprador de la venta de un animal, componiendo con
 * el módulo movimientos (tipo SALIDA_VENTA) para el cambio de estado del
 * animal en vez de reimplementar esa máquina de estados.</p>
 *
 * <p>Las dependencias entre módulos deben realizarse mediante APIs públicas
 * de aplicación o eventos de dominio; nunca accediendo al repositorio o a la
 * infraestructura interna de otro módulo.</p>
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"movimientos", "animales", "shared"}
)
package bo.com.ganadero.ventas;
