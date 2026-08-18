/**
 * Módulo movimientos del monolito modular GANADERO.
 *
 * <p>Las dependencias entre módulos deben realizarse mediante APIs públicas
 * de aplicación o eventos de dominio; nunca accediendo al repositorio o a la
 * infraestructura interna de otro módulo.</p>
 */
@org.springframework.modulith.ApplicationModule(
        type = org.springframework.modulith.ApplicationModule.Type.OPEN,
        allowedDependencies = {"animales", "lotes", "timeline", "alertas::application", "shared"}
)
package bo.com.ganadero.movimientos;
