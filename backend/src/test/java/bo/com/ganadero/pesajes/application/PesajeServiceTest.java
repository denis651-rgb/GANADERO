package bo.com.ganadero.pesajes.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.OrigenAnimal;
import bo.com.ganadero.animales.domain.PropositoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.lotes.domain.EstadoLote;
import bo.com.ganadero.lotes.domain.Lote;
import bo.com.ganadero.lotes.domain.LoteRepository;
import bo.com.ganadero.pesajes.domain.EstadoPesaje;
import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.pesajes.domain.PesajeMasivoResultado;
import bo.com.ganadero.pesajes.domain.PesajeRepository;
import bo.com.ganadero.pesajes.domain.TipoPesaje;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import bo.com.ganadero.alertas.application.MotorAlertas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

class PesajeServiceTest {
    private PesajeRepository pesajes;
    private AnimalRepository animales;
    private LoteRepository lotes;
    private final List<Object> published = new ArrayList<>();
    private UUID company;
    private UUID property;
    private UUID paddock;
    private UUID loteId;
    private UUID animalId;
    private UUID otherAnimalId;
    private UUID userId;
    private PesajeService service;
    private MotorAlertas alertas;

    @BeforeEach
    void setup() {
        published.clear();
        pesajes = mock(PesajeRepository.class);
        animales = mock(AnimalRepository.class);
        lotes = mock(LoteRepository.class);
        company = UUID.randomUUID();
        property = UUID.randomUUID();
        paddock = UUID.randomUUID();
        loteId = UUID.randomUUID();
        animalId = UUID.randomUUID();
        otherAnimalId = UUID.randomUUID();
        userId = UUID.randomUUID();
        alertas = mock(MotorAlertas.class);
        @SuppressWarnings("unchecked") ObjectProvider<MotorAlertas> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(alertas);
        CurrentUser user = new CurrentUser(userId, company, UUID.randomUUID(), Set.of(),
                Set.of("PESAJE_VER", "PESAJE_REGISTRAR", "PESAJE_ANULAR"), Set.of(property), false);
        service = new PesajeService(pesajes, animales, lotes, new UserContext(() -> user),
                published::add, published::add, provider);
        when(animales.validLocation(eq(company), any(), any())).thenReturn(true);
    }

    @Test
    void registraPesajeGuardaContextoHistoricoYPublicaEventos() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        when(pesajes.create(any(Pesaje.class), eq(userId))).thenAnswer(inv -> saved(inv.getArgument(0)));

        Pesaje created = service.registrar(comando(animalId, new BigDecimal("350"), null, null, null, null));

        assertThat(created.pesoKg()).isEqualByComparingTo("350");
        assertThat(created.propiedadId()).isEqualTo(property);
        assertThat(created.potreroId()).isEqualTo(paddock);
        assertThat(created.responsableId()).isEqualTo(userId);
        assertThat(created.estado()).isEqualTo(EstadoPesaje.ACTIVO);
        assertThat(published).anyMatch(e -> e instanceof RegistrarEventoTimeline ev
                && ev.tipo() == TipoEventoAnimal.PESAJE_REGISTRADO && ev.animalId().equals(animalId));
        assertThat(published).anyMatch(e -> e instanceof PesajeAuditEvent ev
                && "REGISTRAR".equals(ev.accion()) && ev.entidadId().equals(created.id()));
        verify(alertas).resolverPorOrigen(company, "ANIMAL", animalId);
    }

    @Test
    void listaPesajesRestringidosALasPropiedadesPermitidas() {
        service.list(null, null, 0, 20);

        verify(pesajes).findAll(company, Set.of(property), false, null, null, 0, 20);
    }

    @Test
    void rechazaPesoNoPositivo() {
        assertThatThrownBy(() -> service.registrar(comando(animalId, BigDecimal.ZERO, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PESAJE_PESO_INVALIDO));
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void rechazaAnimalMuerto() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.MUERTO, property, paddock, null)));
        assertThatThrownBy(() -> service.registrar(comando(animalId, new BigDecimal("300"), null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_NOT_ACTIVE));
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void rechazaAnimalVendido() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.VENDIDO, property, paddock, null)));
        assertThatThrownBy(() -> service.registrar(comando(animalId, new BigDecimal("300"), null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_NOT_ACTIVE));
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void rechazaFechaFutura() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        assertThatThrownBy(() -> service.registrar(comando(animalId, new BigDecimal("300"),
                LocalDate.now().plusDays(1), null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PESAJE_FECHA_INVALIDA));
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void rechazaPropiedadNoAutorizada() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, UUID.randomUUID(), paddock, null)));
        assertThatThrownBy(() -> service.registrar(comando(animalId, new BigDecimal("300"), null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PROPERTY_ACCESS_DENIED));
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void rechazaLoteDeOtraPropiedad() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        UUID otroLote = UUID.randomUUID();
        when(lotes.findById(otroLote, company))
                .thenReturn(Optional.of(lote(otroLote, UUID.randomUUID())));
        assertThatThrownBy(() -> service.registrar(comando(animalId, new BigDecimal("300"),
                null, null, otroLote, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PESAJE_LOTE_INVALIDO));
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void rechazaPotreroDeOtraPropiedad() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, null, null)));
        when(animales.validLocation(eq(company), eq(property), eq(paddock))).thenReturn(false);
        assertThatThrownBy(() -> service.registrar(comando(animalId, new BigDecimal("300"),
                null, paddock, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_ANIMAL_LOCATION));
        verify(pesajes, never()).create(any(), any());
    }

    @Test
    void idempotenciaDevuelveElPesajeYaRegistrado() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        Pesaje existente = pesaje(UUID.randomUUID(), animalId, new BigDecimal("340"), property, paddock, null);
        when(pesajes.create(any(Pesaje.class), eq(userId))).thenReturn(existente);

        Pesaje resultado = service.registrar(comandoConCliente(animalId, new BigDecimal("350"), "CLAVE-1"));

        assertThat(resultado.id()).isEqualTo(existente.id());
        assertThat(resultado.pesoKg()).isEqualByComparingTo("340");
    }

    @Test
    void propagaPesajeDuplicado() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        when(pesajes.create(any(Pesaje.class), eq(userId)))
                .thenThrow(new BusinessException(ErrorCode.PESAJE_DUPLICATED));
        assertThatThrownBy(() -> service.registrar(comando(animalId, new BigDecimal("300"), null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PESAJE_DUPLICATED));
    }

    @Test
    void registrarLoteAplicaMismoPesoYContextoPorAnimal() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(loteId, property)));
        when(pesajes.listActiveAnimalsOfLote(loteId, company))
                .thenReturn(List.of(animalId, otherAnimalId));
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        when(animales.findById(otherAnimalId, company))
                .thenReturn(Optional.of(animal(otherAnimalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        when(pesajes.create(any(Pesaje.class), eq(userId))).thenAnswer(inv -> saved(inv.getArgument(0)));

        List<Pesaje> creados = service.registrarLote(new PesajeLoteCommand(null, loteId,
                LocalDate.now(), new BigDecimal("250"), "WEB", null, null, "Pesaje grupal"));

        assertThat(creados).hasSize(2);
        assertThat(creados).allSatisfy(p -> {
            assertThat(p.loteId()).isEqualTo(loteId);
            assertThat(p.propiedadId()).isEqualTo(property);
            assertThat(p.potreroId()).isEqualTo(paddock);
            assertThat(p.pesoKg()).isEqualByComparingTo("250");
        });
        verify(pesajes, org.mockito.Mockito.times(2)).create(any(Pesaje.class), eq(userId));
        assertThat(published).anyMatch(e -> e instanceof PesajeAuditEvent ev
                && "REGISTRAR_LOTE".equals(ev.accion()));
    }

    @Test
    void registrarMasivoReintentaSoloLosErrores() {
        when(animales.findById(animalId, company))
                .thenReturn(Optional.of(animal(animalId, EstadoAnimal.ACTIVO, property, paddock, null)));
        when(animales.findById(otherAnimalId, company))
                .thenReturn(Optional.of(animal(otherAnimalId, EstadoAnimal.MUERTO, property, paddock, null)));
        when(pesajes.create(any(Pesaje.class), eq(userId))).thenAnswer(inv -> saved(inv.getArgument(0)));

        PesajeMasivoCommand command = new PesajeMasivoCommand(LocalDate.now(), "WEB", null, List.of(
                new PesajeMasivoItem(UUID.randomUUID(), animalId, null, new BigDecimal("300"),
                        TipoPesaje.RUTINA, null, null, null, null, null, null),
                new PesajeMasivoItem(UUID.randomUUID(), otherAnimalId, null, new BigDecimal("300"),
                        TipoPesaje.RUTINA, null, null, null, null, null, null)));

        List<PesajeMasivoResultado> resultados = service.registrarMasivo(command);

        assertThat(resultados).hasSize(2);
        assertThat(resultados.stream().filter(PesajeMasivoResultado::ok).toList()).hasSize(1);
        assertThat(resultados.stream().filter(r -> !r.ok()).toList()).singleElement()
                .satisfies(r -> assertThat(r.errorCode()).isEqualTo(ErrorCode.ANIMAL_NOT_ACTIVE.name()));
        verify(pesajes, org.mockito.Mockito.times(1)).create(any(Pesaje.class), eq(userId));
    }

    @Test
    void anulaConMotivoYVersionGuardaYPublica() {
        Pesaje activo = pesaje(UUID.randomUUID(), animalId, new BigDecimal("300"), property, paddock, null);
        when(pesajes.findById(activo.id(), company)).thenReturn(Optional.of(activo));
        Pesaje anulado = pesaje(activo.id(), animalId, new BigDecimal("300"), property, paddock, EstadoPesaje.ANULADO);
        when(pesajes.annul(activo.id(), company, "Error de tipeo", 0, userId)).thenReturn(anulado);

        Pesaje resultado = service.anular(activo.id(), "Error de tipeo", 0);

        assertThat(resultado.estado()).isEqualTo(EstadoPesaje.ANULADO);
        assertThat(published).anyMatch(e -> e instanceof RegistrarEventoTimeline ev
                && ev.tipo() == TipoEventoAnimal.PESAJE_ANULADO && ev.animalId().equals(animalId));
        assertThat(published).anyMatch(e -> e instanceof PesajeAuditEvent ev
                && "ANULAR".equals(ev.accion()));
    }

    @Test
    void anulaRechazaMotivoEnBlanco() {
        Pesaje activo = pesaje(UUID.randomUUID(), animalId, new BigDecimal("300"), property, paddock, null);
        when(pesajes.findById(activo.id(), company)).thenReturn(Optional.of(activo));
        assertThatThrownBy(() -> service.anular(activo.id(), "   ", 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PESAJE_MOTIVO_REQUERIDO));
        verify(pesajes, never()).annul(any(), any(), any(), anyLong(), any());
    }

    @Test
    void anulaRechazaPesajeYaAnulado() {
        Pesaje anulado = pesaje(UUID.randomUUID(), animalId, new BigDecimal("300"), property, paddock, EstadoPesaje.ANULADO);
        when(pesajes.findById(anulado.id(), company)).thenReturn(Optional.of(anulado));
        assertThatThrownBy(() -> service.anular(anulado.id(), "Motivo", 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PESAJE_ALREADY_ANNULLED));
        verify(pesajes, never()).annul(any(), any(), any(), anyLong(), any());
    }

    @Test
    void anulaValidaAccesoSobreLaPropiedadHistoricaDelPesaje() {
        UUID otraPropiedad = UUID.randomUUID();
        Pesaje activo = pesaje(UUID.randomUUID(), animalId, new BigDecimal("300"), otraPropiedad, paddock, null);
        when(pesajes.findById(activo.id(), company)).thenReturn(Optional.of(activo));
        assertThatThrownBy(() -> service.anular(activo.id(), "Motivo", 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PROPERTY_ACCESS_DENIED));
        verify(pesajes, never()).annul(any(), any(), any(), anyLong(), any());
    }

    private PesajeCommand comando(UUID animal, BigDecimal peso, LocalDate fecha, UUID potrero, UUID lote,
                                  UUID clienteUuid) {
        return new PesajeCommand(null, animal, fecha, peso, null, null, null, null,
                null, potrero, lote, "WEB", clienteUuid,
                clienteUuid == null ? null : clienteUuid.toString(), null);
    }

    private PesajeCommand comandoConCliente(UUID animal, BigDecimal peso, String idempotencyKey) {
        UUID cliente = UUID.fromString("00000000-0000-0000-0000-000000000001");
        return new PesajeCommand(null, animal, LocalDate.now(), peso, null, null, null, null,
                null, null, null, "WEB", cliente, idempotencyKey, null);
    }

    private Pesaje saved(Pesaje value) {
        return new Pesaje(value.id(), value.empresaId(), value.animalId(), value.fecha(), value.pesoKg(),
                value.tipo(), value.condicionCorporal(), value.bascula(), value.responsableId(),
                value.propiedadId(), value.potreroId(), value.loteId(), value.dispositivo(),
                value.clienteUuid(), value.idempotencyKey(), value.estado(), value.motivoAnulacion(),
                value.anuladoPor(), value.fechaAnulacion(), value.observaciones(),
                "A-1", "Animal 1", value.loteNombre(), value.potreroNombre(), value.propiedadNombre(),
                value.responsableNombre(), value.version());
    }

    private Pesaje pesaje(UUID id, UUID animal, BigDecimal peso, UUID prop, UUID potrero, EstadoPesaje estado) {
        return new Pesaje(id, company, animal, LocalDate.now(), peso, TipoPesaje.RUTINA, null, null,
                userId, prop, potrero, null, "WEB", UUID.randomUUID(), null,
                estado == null ? EstadoPesaje.ACTIVO : estado, null, null, null, null,
                "A-1", "Animal 1", null, null, prop == null ? null : "Propiedad", null, 0);
    }

    private Animal animal(UUID id, EstadoAnimal state, UUID prop, UUID potrero, UUID lote) {
        return new Animal(id, company, "A-1", "Animal 1", SexoAnimal.HEMBRA, null, false,
                UUID.randomUUID(), UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO,
                prop, potrero, lote, state, LocalDate.now(), null, null, null, null, null, 0);
    }

    private Lote lote(UUID id, UUID prop) {
        return new Lote(id, company, prop, "L-1", "Lote 1", null, EstadoLote.ACTIVO, LocalDate.now(), null, 0);
    }
}
