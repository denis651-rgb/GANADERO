package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.sanidad.domain.LugarAplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConfirmarJornadaCommand(UUID operationId, long version, UUID planItemId, BigDecimal dosisAplicada,
                                      String motivoAjusteDosis, String unidadDosis, String productoAplicadoTexto,
                                      String motivoCambioProducto, String viaAdministracion,
                                      LugarAplicacion lugarAplicacion, LocalDate fechaAplicacion, String resultado,
                                      String observaciones, Integer retiroCarneDias, Integer retiroLecheDias) {
}
