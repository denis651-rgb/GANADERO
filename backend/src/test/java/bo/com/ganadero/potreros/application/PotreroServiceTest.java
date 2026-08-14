package bo.com.ganadero.potreros.application;

import bo.com.ganadero.potreros.domain.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PotreroServiceTest {
    @Test
    void rejectsSectorFromAnotherProperty() {
        UUID company = UUID.randomUUID(), property = UUID.randomUUID(), sector = UUID.randomUUID();
        PotreroRepository repo = mock(PotreroRepository.class);
        when(repo.propertyExists(property, company)).thenReturn(true);
        when(repo.sectorBelongs(sector, property, company)).thenReturn(false);
        CurrentUser user = new CurrentUser(UUID.randomUUID(), company, UUID.randomUUID(), Set.of(),
                Set.of("POTRERO_CREAR"), Set.of(), true);
        PotreroService service = new PotreroService(repo, mock(TipoPastoRepository.class),
                new UserContext(() -> user), event -> {}, mock(CodigoService.class));
        PotreroCommand command = new PotreroCommand(property, sector, "P-1", "Potrero", null, null,
                null, false, null, null, true, false, false, false, false, 0L);

        assertThatThrownBy(() -> service.create(command)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.code()).isEqualTo(ErrorCode.SECTOR_NOT_FOUND));
        verify(repo, never()).create(any(), any());
    }

    @Test
    void removesOptionalValuesWhenExplicitlyRequested() {
        UUID company = UUID.randomUUID(), property = UUID.randomUUID(), sector = UUID.randomUUID();
        UUID grass = UUID.randomUUID(), id = UUID.randomUUID();
        PotreroRepository repo = mock(PotreroRepository.class);
        Potrero old = new Potrero(id, company, property, sector, "P-1", "Potrero",
                java.math.BigDecimal.TEN, grass, java.math.BigDecimal.ONE, true,
                EstadoPotrero.DISPONIBLE, null, true, 4L);
        when(repo.findById(id, company)).thenReturn(Optional.of(old));
        when(repo.propertyExists(property, company)).thenReturn(true);
        when(repo.update(any(), any(), eq(true), eq(true), eq(true), eq(true)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        CurrentUser user = new CurrentUser(UUID.randomUUID(), company, UUID.randomUUID(), Set.of(),
                Set.of("POTRERO_EDITAR"), Set.of(), true);
        CodigoService codes = mock(CodigoService.class);
        when(codes.paraActualizacion(any(), eq(TipoCodigo.POTRERO), eq(property), isNull(),
                eq("P-1"), eq("P-1"))).thenReturn("P-1");
        PotreroService service = new PotreroService(repo, mock(TipoPastoRepository.class),
                new UserContext(() -> user), event -> {}, codes);
        PotreroCommand command = new PotreroCommand(null, null, "P-1", "Potrero", null, null,
                null, true, EstadoPotrero.DISPONIBLE, null, true, true, true, true, true, 4L);

        Potrero updated = service.update(id, command);

        assertThat(updated.sectorId()).isNull();
        assertThat(updated.tipoPastoId()).isNull();
        assertThat(updated.superficieHa()).isNull();
        assertThat(updated.capacidadUa()).isNull();
        verify(repo).update(any(), any(), eq(true), eq(true), eq(true), eq(true));
    }
}
