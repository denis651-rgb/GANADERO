package bo.com.ganadero.seguridad.application;

public record ActualizarMiembroCommand(String nombres, String apellidos, String telefono,
        String avatarPath, String cargo, Boolean accesoTodasPropiedades,
        long perfilVersion, long miembroVersion) {}
