package bo.com.ganadero.movimientos.api;

import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.EstadoMovimiento;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.movimientos.domain.TipoMovimiento;
import bo.com.ganadero.shared.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * INGRESO_COMPRA/SALIDA_VENTA ya se crean internamente desde CompraService/VentaService
 * (que llaman a MovimientoService directamente, sin pasar por este controller); por eso la
 * restricción vive aquí y no en el servicio, para no romper esas llamadas internas.
 */
class MovimientoControllerTest {
    private final MovimientoService service = mock(MovimientoService.class);
    private final MovimientoController controller = new MovimientoController(service);
    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void rechazaCrearUnMovimientoDeIngresoPorCompraDesdeElEndpointGenerico() {
        CrearMovimientoRequest body = request(TipoMovimiento.INGRESO_COMPRA);

        assertThatThrownBy(() -> controller.create(body, request))
                .isInstanceOf(bo.com.ganadero.shared.error.BusinessException.class)
                .extracting(ex -> ((bo.com.ganadero.shared.error.BusinessException) ex).code())
                .isEqualTo(bo.com.ganadero.shared.error.ErrorCode.MOVEMENT_TIPO_RESTRINGIDO);
        verify(service, never()).create(any());
    }

    @Test
    void rechazaCrearUnMovimientoDeSalidaPorVentaDesdeElEndpointGenerico() {
        CrearMovimientoRequest body = request(TipoMovimiento.SALIDA_VENTA);

        assertThatThrownBy(() -> controller.create(body, request))
                .isInstanceOf(bo.com.ganadero.shared.error.BusinessException.class)
                .extracting(ex -> ((bo.com.ganadero.shared.error.BusinessException) ex).code())
                .isEqualTo(bo.com.ganadero.shared.error.ErrorCode.MOVEMENT_TIPO_RESTRINGIDO);
        verify(service, never()).create(any());
    }

    @Test
    void permiteCrearUnCambioDePotreroDesdeElEndpointGenerico() {
        CrearMovimientoRequest body = request(TipoMovimiento.CAMBIO_POTRERO);
        UUID id = UUID.randomUUID();
        Movimiento creado = new Movimiento(id, UUID.randomUUID(), TipoMovimiento.CAMBIO_POTRERO,
                EstadoMovimiento.PENDIENTE, LocalDate.now(), "Motivo", null, null, null, null, null, null, null,
                UUID.randomUUID(), null, null, null, null, null, null, null, null, null, null, 0);
        when(service.create(any())).thenReturn(creado);

        ApiResponse<MovimientoResponse> response = controller.create(body, request);

        assertThat(response.data().id()).isEqualTo(id);
        verify(service).create(any());
    }

    private CrearMovimientoRequest request(TipoMovimiento tipo) {
        return new CrearMovimientoRequest(tipo, LocalDate.now(), "Motivo", null, UUID.randomUUID(),
                UUID.randomUUID(), null, UUID.randomUUID(), UUID.randomUUID(), null,
                List.of(new CrearMovimientoRequest.MovimientoAnimalRequest(UUID.randomUUID(), 0L)));
    }
}
