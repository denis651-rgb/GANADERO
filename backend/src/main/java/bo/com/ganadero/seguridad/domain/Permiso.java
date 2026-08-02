package bo.com.ganadero.seguridad.domain;

import java.util.UUID;

public record Permiso(UUID id, String codigo, String nombre, String descripcion,
                      String modulo, boolean activo) {}
