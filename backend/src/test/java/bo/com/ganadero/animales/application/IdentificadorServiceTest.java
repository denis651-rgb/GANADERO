package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentificadorServiceTest {
    private IdentificadorRepository identificadores;
    private AnimalRepository animales;
    private UUID company;
    private UUID property;
    private UUID animalId;
    private UUID userId;
    private IdentificadorService service;

    @BeforeEach
    void setup() {
        identificadores = mock(IdentificadorRepository.class);
        animales = mock(AnimalRepository.class);
        company = UUID.randomUUID();
        property = UUID.randomUUID();
        animalId = UUID.randomUUID();
        userId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, company, UUID.randomUUID(), Set.of(),
                Set.of("IDENTIFICADOR_VER", "IDENTIFICADOR_ASIGNAR", "IDENTIFICADOR_RETIRAR"),
                Set.of(), true);
        service = new IdentificadorService(identificadores, animales, new IdentificadorValueNormalizer(),
                new UserContext(() -> user), event -> {}, event -> {});
    }

    @Test
    void assignRejectsManualQr() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        IdentificadorCommand command = new IdentificadorCommand(null, TipoIdentificador.QR, "manual", false, null, null);
        assertThatThrownBy(() -> service.assign(animalId, command))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_QR_MANUAL_NOT_ALLOWED));
        verify(identificadores, never()).create(any(), any());
    }

    @Test
    void assignNormalizesValueAndSetsPrincipal() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.create(any(), eq(userId))).thenAnswer(invocation -> invocation.getArgument(0));
        IdentificadorCommand command = new IdentificadorCommand(null, TipoIdentificador.ARETE, " ar-001 ", true, null, null);
        IdentificadorAnimal saved = service.assign(animalId, command);
        assertThat(saved.valor()).isEqualTo("AR-001");
        assertThat(saved.principal()).isTrue();
        verify(identificadores).lockActiveIdentifiers(animalId, company);
        verify(identificadores).clearPrincipal(animalId, company, null);
    }

    @Test
    void assignRejectsInactiveAnimal() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.VENDIDO)));
        IdentificadorCommand command = new IdentificadorCommand(null, TipoIdentificador.ARETE, "AR-001", false, null, null);
        assertThatThrownBy(() -> service.assign(animalId, command))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_NOT_ACTIVE));
        verify(identificadores, never()).create(any(), any());
    }

    @Test
    void updateRejectsTipoChange() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(identificadorId(), animalId, company))
                .thenReturn(Optional.of(identificador(TipoIdentificador.ARETE, "AR-001", false, EstadoIdentificador.ACTIVO, 0)));
        IdentificadorCommand command = new IdentificadorCommand(null, TipoIdentificador.RFID, "1234", null, null, 0L);
        assertThatThrownBy(() -> service.update(animalId, identificadorId(), command))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_INVALID_VALUE));
        verify(identificadores, never()).update(any(), any());
    }

    @Test
    void updateRejectsRetired() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(identificadorId(), animalId, company))
                .thenReturn(Optional.of(identificador(TipoIdentificador.ARETE, "AR-001", false, EstadoIdentificador.RETIRADO, 1)));
        IdentificadorCommand command = new IdentificadorCommand(null, null, "AR-002", null, null, 1L);
        assertThatThrownBy(() -> service.update(animalId, identificadorId(), command))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_ALREADY_RETIRED));
        verify(identificadores, never()).update(any(), any());
    }

    @Test
    void updateUsesClientVersionAndClearsPreviousPrincipal() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(identificadorId(), animalId, company))
                .thenReturn(Optional.of(identificador(TipoIdentificador.ARETE, "AR-001", false, EstadoIdentificador.ACTIVO, 2)));
        when(identificadores.update(any(), eq(userId))).thenAnswer(invocation -> invocation.getArgument(0));
        IdentificadorCommand command = new IdentificadorCommand(null, null, "AR-002", true, null, 2L);
        IdentificadorAnimal saved = service.update(animalId, identificadorId(), command);
        assertThat(saved.valor()).isEqualTo("AR-002");
        assertThat(saved.version()).isEqualTo(2);
        verify(identificadores).lockActiveIdentifiers(animalId, company);
        verify(identificadores).clearPrincipal(animalId, company, identificadorId());
    }

    @Test
    void makePrincipalClearsOthersAndSetsTarget() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(identificadorId(), animalId, company))
                .thenReturn(Optional.of(identificador(TipoIdentificador.ARETE, "AR-001", false, EstadoIdentificador.ACTIVO, 2)));
        when(identificadores.setPrincipal(identificadorId(), animalId, company, 2, userId))
                .thenAnswer(invocation -> identificador(TipoIdentificador.ARETE, "AR-001", true, EstadoIdentificador.ACTIVO, 3));
        IdentificadorAnimal saved = service.makePrincipal(animalId, identificadorId(), 2);
        assertThat(saved.principal()).isTrue();
        verify(identificadores).lockActiveIdentifiers(animalId, company);
        verify(identificadores).clearPrincipal(animalId, company, identificadorId());
    }

    @Test
    void makePrincipalIsNoopWhenAlreadyPrincipal() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(identificadorId(), animalId, company))
                .thenReturn(Optional.of(identificador(TipoIdentificador.ARETE, "AR-001", true, EstadoIdentificador.ACTIVO, 2)));
        IdentificadorAnimal saved = service.makePrincipal(animalId, identificadorId(), 2);
        assertThat(saved.principal()).isTrue();
        verify(identificadores, never()).setPrincipal(any(), any(), any(), anyLong(), any());
    }

    @Test
    void retireRejectsDoubleRetire() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(identificadorId(), animalId, company))
                .thenReturn(Optional.of(identificador(TipoIdentificador.ARETE, "AR-001", false, EstadoIdentificador.RETIRADO, 1)));
        assertThatThrownBy(() -> service.retire(animalId, identificadorId(), "Arete perdido", 1))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_ALREADY_RETIRED));
        verify(identificadores, never()).retire(any(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void retireRejectsBlankMotivo() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        assertThatThrownBy(() -> service.retire(animalId, identificadorId(), "   ", 0))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        verify(identificadores, never()).retire(any(), any(), any(), any(), anyLong(), any());
    }

    private Animal animal(EstadoAnimal state) {
        return new Animal(animalId, company, "A-1", null, SexoAnimal.HEMBRA, null, false,
                UUID.randomUUID(), UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO,
                property, UUID.randomUUID(), null, state, LocalDate.now(), null, null, null, null, null, 0);
    }

    private UUID identificadorId() {
        return UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    }

    private IdentificadorAnimal identificador(TipoIdentificador tipo, String valor, boolean principal,
                                              EstadoIdentificador estado, long version) {
        Instant now = Instant.now();
        return new IdentificadorAnimal(identificadorId(), company, animalId, tipo, valor, principal, estado,
                now, null, null, userId, null, null, null, now, now, version);
    }
}
