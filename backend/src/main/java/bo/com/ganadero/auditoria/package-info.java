/**
 * Módulo auditoria del monolito modular GANADERO.
 *
 * <p>Las dependencias entre módulos deben realizarse mediante APIs públicas
 * de aplicación o eventos de dominio; nunca accediendo al repositorio o a la
 * infraestructura interna de otro módulo.</p>
 */
@org.springframework.modulith.ApplicationModule(
        allowedDependencies = {"animales", "propiedades", "potreros", "seguridad", "lotes", "movimientos", "pesajes", "archivos", "reproduccion", "sanidad", "alertas::application", "shared"}
)
package bo.com.ganadero.auditoria;
