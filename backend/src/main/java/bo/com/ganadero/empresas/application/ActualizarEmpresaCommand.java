package bo.com.ganadero.empresas.application;

public record ActualizarEmpresaCommand(String razonSocial, String nombreComercial, String nit,
                                       String telefono, String email, String direccion, long version) {}
