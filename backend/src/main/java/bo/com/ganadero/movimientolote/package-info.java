/**
 * Módulo movimientolote del monolito modular GANADERO: operación "Mover lote" (traslado
 * colectivo, total o parcial, de los miembros de un lote ganadero).
 *
 * <p>Las dependencias entre módulos deben realizarse mediante APIs públicas
 * de aplicación o eventos de dominio; nunca accediendo al repositorio o a la
 * infraestructura interna de otro módulo.</p>
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"lotes", "movimientos", "animales", "potreros", "propiedades", "timeline",
                "alertas::application", "shared"}
)
package bo.com.ganadero.movimientolote;
