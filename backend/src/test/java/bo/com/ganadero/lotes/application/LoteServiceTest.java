package bo.com.ganadero.lotes.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.lotes.domain.MembresiaLote;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

class LoteServiceTest {
    private LoteRepository lotes;
    private AnimalRepository animales;
    private UUID company;
    private UUID property;
    private UUID otherProperty;
    private UUID loteId;
    private UUID otherLoteId;
    private UUID animalId;
    private UUID userId;
    private LoteService service;

    @BeforeEach
    void setup() {
        lotes = mock(LoteRepository.class);
        animales = mock(AnimalRepository.class);
        company = UUID.randomUUID();
        property = UUID.randomUUID();
        otherProperty = UUID.randomUUID();
        loteId = UUID.randomUUID();
        otherLoteId = UUID.randomUUID();
        animalId = UUID.randomUUID();
        userId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(userId, company, UUID.randomUUID(), Set.of(),
                Set.of("LOTE_VER", "LOTE_CREAR", "LOTE_EDITAR", "LOTE_ASIGNAR_ANIMALES"),
                Set.of(), true);
        service = new LoteService(lotes, animales, new UserContext(() -> user), event -> {}, event -> {});
    }

    @Test
    void addAnimalsRejectsClosedLot() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.CERRADO)));
        assertThatThrownBy(() -> service.addAnimals(loteId, ingreso(List.of(animalId), null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.LOT_CLOSED));
        verify(lotes, never()).openMembership(any(), any(), any(), any());
    }

    @Test
    void addAnimalsRejectsDuplicateRequest() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        assertThatThrownBy(() -> service.addAnimals(loteId, ingreso(List.of(animalId, animalId), null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.DUPLICATE_ANIMAL_IN_REQUEST));
    }

    @Test
    void addAnimalsRejectsFutureIngreso() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        assertThatThrownBy(() -> service.addAnimals(loteId,
                new IngresoLoteCommand(List.of(animalId), null, Instant.now().plusSeconds(60), null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_MEMBERSHIP_DATE));
    }

    @Test
    void addAnimalsAtomicoRejectsInactiveAnimal() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.VENDIDO)));
        assertThatThrownBy(() -> service.addAnimals(loteId,
                new IngresoLoteCommand(List.of(animalId), "ATOMICO", null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_STATUS_NOT_ALLOWED));
        verify(lotes, never()).openMembership(any(), any(), any(), any());
    }

    @Test
    void addAnimalsParcialRecordsErrorForInactiveAnimal() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.MUERTO)));
        IngresoMasivoResultado result = service.addAnimals(loteId, ingreso(List.of(animalId), "PARCIAL"));
        assertThat(result.ok()).isFalse();
        assertThat(result.ingresados()).isZero();
        assertThat(result.resultados()).singleElement().satisfies(r -> {
            assertThat(r.animalId()).isEqualTo(animalId);
            assertThat(r.estado()).isEqualTo("ERROR");
            assertThat(r.mensaje()).isNotBlank();
        });
    }

    @Test
    void addAnimalsRejectsPropertyMismatch() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO, otherProperty)));
        IngresoMasivoResultado result = service.addAnimals(loteId, ingreso(List.of(animalId), "PARCIAL"));
        assertThat(result.ok()).isFalse();
        assertThat(result.resultados()).singleElement().satisfies(r ->
                assertThat(r.estado()).isEqualTo("ERROR"));
        verify(lotes, never()).openMembership(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void addAnimalsIngressesNewAnimal() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(lotes.findActiveMembership(animalId, company)).thenReturn(Optional.empty());
        when(lotes.openMembership(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(membresia(loteId, animalId));
        IngresoMasivoResultado result = service.addAnimals(loteId, ingreso(List.of(animalId), null));
        assertThat(result.ok()).isTrue();
        assertThat(result.ingresados()).isEqualTo(1);
        verify(lotes).openMembership(any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(animales).updateLote(animalId, company, loteId, userId);
    }

    @Test
    void addAnimalsAutoMovesFromPreviousLot() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(lotes.findById(otherLoteId, company)).thenReturn(Optional.of(lote(otherLoteId, EstadoLote.ACTIVO)));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(lotes.findActiveMembership(animalId, company)).thenReturn(Optional.of(membresia(otherLoteId, animalId)));
        when(lotes.openMembership(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(membresia(loteId, animalId));
        IngresoMasivoResultado result = service.addAnimals(loteId, ingreso(List.of(animalId), null));
        assertThat(result.ok()).isTrue();
        verify(lotes).closeMembership(eq(otherLoteId), any(), eq(animalId), eq(company), eq("CAMBIO_LOTE"),
                any(), eq(userId));
        verify(lotes).openMembership(eq(loteId), any(), eq(animalId), eq(company), any(), any(), any(), any(), eq(userId));
        verify(animales).updateLote(animalId, company, loteId, userId);
    }

    @Test
    void addAnimalsRejectsAnimalAlreadyInTargetLot() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(lotes.findActiveMembership(animalId, company)).thenReturn(Optional.of(membresia(loteId, animalId)));
        IngresoMasivoResultado result = service.addAnimals(loteId, ingreso(List.of(animalId), "PARCIAL"));
        assertThat(result.ok()).isFalse();
        assertThat(result.resultados()).singleElement().satisfies(r ->
                assertThat(r.estado()).isEqualTo("ERROR"));
        verify(lotes, never()).openMembership(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void removeAnimalsRejectsAnimalNotInLote() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(lotes.findActiveMembership(animalId, company)).thenReturn(Optional.empty());
        RetiroMasivoResultado result = service.removeAnimals(loteId, retiro(List.of(animalId), null));
        assertThat(result.ok()).isFalse();
        assertThat(result.resultados()).singleElement().satisfies(r ->
                assertThat(r.estado()).isEqualTo("ERROR"));
    }

    @Test
    void removeAnimalsRejectsAnimalInOtherLote() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(lotes.findActiveMembership(animalId, company)).thenReturn(Optional.of(membresia(otherLoteId, animalId)));
        RetiroMasivoResultado result = service.removeAnimals(loteId, retiro(List.of(animalId), "Venta"));
        assertThat(result.ok()).isFalse();
        verify(animales, never()).updateLote(any(), any(), any(), any());
    }

    @Test
    void removeAnimalsRejectsInvalidFechaSalida() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(lotes.findActiveMembership(animalId, company)).thenReturn(Optional.of(membresia(loteId, animalId)));
        RetiroMasivoResultado result = service.removeAnimals(loteId,
                new RetiroLoteCommand(List.of(animalId), Instant.now().minusSeconds(60), "Venta"));
        assertThat(result.ok()).isFalse();
        assertThat(result.resultados()).singleElement().satisfies(r ->
                assertThat(r.estado()).isEqualTo("ERROR"));
        verify(animales, never()).updateLote(any(), any(), any(), any());
    }

    @Test
    void removeAnimalsSuccess() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(lotes.findActiveMembership(animalId, company)).thenReturn(Optional.of(membresia(loteId, animalId)));
        RetiroMasivoResultado result = service.removeAnimals(loteId, retiro(List.of(animalId), "Venta"));
        assertThat(result.ok()).isTrue();
        assertThat(result.retirados()).isEqualTo(1);
        verify(lotes).closeMembership(eq(loteId), any(), eq(animalId), eq(company), eq("Venta"),
                any(), eq(userId));
        verify(animales).updateLote(animalId, company, null, userId);
    }

    @Test
    void closeRejectsActiveAnimals() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(lotes.hasActiveAnimals(loteId, company)).thenReturn(true);
        assertThatThrownBy(() -> service.close(loteId, 0, null, null))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.LOT_HAS_ACTIVE_ANIMALS));
        verify(lotes, never()).close(any(), any(), anyLong(), any(), any(), any());
    }

    @Test
    void closeRejectsAlreadyClosedLot() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.CERRADO)));
        assertThatThrownBy(() -> service.close(loteId, 1, null, null))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.LOT_ALREADY_CLOSED));
    }

    @Test
    void closeSuccess() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(EstadoLote.ACTIVO)));
        when(lotes.hasActiveAnimals(loteId, company)).thenReturn(false);
        when(lotes.close(loteId, company, 0, null, "Fin de ciclo", userId)).thenReturn(lote(EstadoLote.CERRADO));
        Lote closed = service.close(loteId, 0, null, "Fin de ciclo");
        assertThat(closed.estado()).isEqualTo(EstadoLote.CERRADO);
        verify(lotes).close(loteId, company, 0, null, "Fin de ciclo", userId);
    }

    private IngresoLoteCommand ingreso(List<UUID> ids, String modo) {
        return new IngresoLoteCommand(ids, modo, null, null, null);
    }

    private RetiroLoteCommand retiro(List<UUID> ids, String motivo) {
        return new RetiroLoteCommand(ids, null, motivo);
    }

    private Lote lote(EstadoLote estado) {
        return lote(loteId, estado);
    }

    private Lote lote(UUID id, EstadoLote estado) {
        return new Lote(id, company, property, "L-1", "Lote 1", null, estado, LocalDate.now(), null, 0);
    }

    private Animal animal(EstadoAnimal state) {
        return animal(state, property);
    }

    private Animal animal(EstadoAnimal state, UUID propertyId) {
        return new Animal(animalId, company, "A-1", null, SexoAnimal.HEMBRA, null, false,
                UUID.randomUUID(), UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO,
                propertyId, UUID.randomUUID(), null, state, LocalDate.now(), null, null, null, null, null, 0);
    }

    private MembresiaLote membresia(UUID lot, UUID animal) {
        return new MembresiaLote(UUID.randomUUID(), lot, animal, Instant.now(), null, null, null, null,
                "PARCIAL", userId, null, 0);
    }
}
