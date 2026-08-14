/**
 * Módulo sanidad del monolito modular GANADERO.
 *
 * <p>Las dependencias entre módulos deben realizarse mediante APIs públicas
 * de aplicación o eventos de dominio; nunca accediendo al repositorio o a la
 * infraestructura interna de otro módulo.</p>
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package bo.com.ganadero.sanidad;
