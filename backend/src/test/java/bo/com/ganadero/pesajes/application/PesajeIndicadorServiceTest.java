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
import bo.com.ganadero.pesajes.domain.PesajeIndicadorAnimal;
import bo.com.ganadero.pesajes.domain.PesajeIndicadorLote;
import bo.com.ganadero.pesajes.domain.PesajeRepository;
import bo.com.ganadero.pesajes.domain.PesajeSinPesaje;
import bo.com.ganadero.pesajes.domain.TipoPesaje;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PesajeIndicadorServiceTest {
    private PesajeRepository pesajes;
    private AnimalRepository animales;
    private LoteRepository lotes;
    private UUID company;
    private UUID property;
    private UUID loteId;
    private UUID animalId;
    private PesajeIndicadorService service;

    @BeforeEach
    void setup() {
        pesajes = mock(PesajeRepository.class);
        animales = mock(AnimalRepository.class);
        lotes = mock(LoteRepository.class);
        company = UUID.randomUUID();
        property = UUID.randomUUID();
        loteId = UUID.randomUUID();
        animalId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(UUID.randomUUID(), company, UUID.randomUUID(),
                Set.of(), Set.of("PESAJE_VER"), Set.of(property), false);
        service = new PesajeIndicadorService(pesajes, animales, lotes, new UserContext(() -> user));
    }

    @Test
    void indicadorAnimalCalculaVariacionGananciaDiariaYComparacionVsLote() {
        LocalDate anteriorFecha = LocalDate.of(2026, 8, 1);
        LocalDate ultimoFecha = LocalDate.of(2026, 8, 10);
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(property, loteId)));
        when(pesajes.findByAnimal(animalId, company)).thenReturn(List.of(
                pesaje(ultimoFecha, new BigDecimal("360"), TipoPesaje.RUTINA),
                pesaje(anteriorFecha, new BigDecimal("330"), TipoPesaje.RUTINA)));
        when(pesajes.indicadorLote(loteId, company))
                .thenReturn(Optional.of(new PesajeIndicadorLote(loteId, "L-1", "Lote 1", 10, 8, 2,
                        new BigDecimal("350"), new BigDecimal("300"), new BigDecimal("400"),
                        anteriorFecha, ultimoFecha)));

        PesajeIndicadorAnimal indicador = service.indicadorAnimal(animalId);

        assertThat(indicador.ultimoPesoKg()).isEqualByComparingTo("360");
        assertThat(indicador.fechaUltimoPesaje()).isEqualTo(ultimoFecha);
        assertThat(indicador.pesoAnteriorKg()).isEqualByComparingTo("330");
        assertThat(indicador.variacionKg()).isEqualByComparingTo("30");
        assertThat(indicador.variacionPct()).isEqualByComparingTo(new BigDecimal("9.09"));
        assertThat(indicador.gananciaDiariaKg()).isEqualByComparingTo(new BigDecimal("3.333"));
        assertThat(indicador.promedioLoteKg()).isEqualByComparingTo("350");
        assertThat(indicador.animalesPesadosLote()).isEqualTo(8);
        assertThat(indicador.diferenciaVsLoteKg()).isEqualByComparingTo("10");
        assertThat(indicador.diferenciaVsLotePct()).isEqualByComparingTo(new BigDecimal("2.86"));
        assertThat(indicador.evolucion()).hasSize(2);
        assertThat(indicador.evolucion().get(0).fecha()).isEqualTo(anteriorFecha);
        assertThat(indicador.evolucion().get(1).fecha()).isEqualTo(ultimoFecha);
    }

    @Test
    void indicadorAnimalSinPesajesDevuelveNulosYEvolucionVacia() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(property, null)));
        when(pesajes.findByAnimal(animalId, company)).thenReturn(List.of());

        PesajeIndicadorAnimal indicador = service.indicadorAnimal(animalId);

        assertThat(indicador.ultimoPesoKg()).isNull();
        assertThat(indicador.variacionKg()).isNull();
        assertThat(indicador.gananciaDiariaKg()).isNull();
        assertThat(indicador.promedioLoteKg()).isNull();
        assertThat(indicador.evolucion()).isEmpty();
        verify(pesajes, never()).indicadorLote(anyUUID(), anyUUID());
    }

    @Test
    void indicadorAnimalExcluyePesajesAnulados() {
        LocalDate ultimoFecha = LocalDate.of(2026, 8, 10);
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(property, null)));
        when(pesajes.findByAnimal(animalId, company)).thenReturn(List.of(
                pesaje(ultimoFecha, new BigDecimal("360"), TipoPesaje.RUTINA),
                pesajeAnulado(ultimoFecha.minusDays(5), new BigDecimal("380"))));

        PesajeIndicadorAnimal indicador = service.indicadorAnimal(animalId);

        assertThat(indicador.ultimoPesoKg()).isEqualByComparingTo("360");
        assertThat(indicador.pesoAnteriorKg()).isNull();
        assertThat(indicador.variacionKg()).isNull();
    }

    @Test
    void indicadorAnimalRechazaAnimalDeOtraEmpresa() {
        when(animales.findById(animalId, company)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.indicadorAnimal(animalId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_NOT_FOUND));
    }

    @Test
    void indicadorAnimalValidaAccesoALaPropiedadDelAnimal() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(UUID.randomUUID(), null)));
        assertThatThrownBy(() -> service.indicadorAnimal(animalId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PROPERTY_ACCESS_DENIED));
    }

    @Test
    void indicadorLoteDevuelveDatosConPropiedadAutorizada() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(property)));
        when(pesajes.indicadorLote(loteId, company))
                .thenReturn(Optional.of(new PesajeIndicadorLote(loteId, "L-1", "Lote 1", 10, 8, 2,
                        new BigDecimal("350"), new BigDecimal("300"), new BigDecimal("400"),
                        LocalDate.of(2026, 7, 1), LocalDate.of(2026, 8, 10))));

        PesajeIndicadorLote indicador = service.indicadorLote(loteId);

        assertThat(indicador.pesoPromedioKg()).isEqualByComparingTo("350");
        assertThat(indicador.animalesTotales()).isEqualTo(10);
        assertThat(indicador.animalesSinPesaje()).isEqualTo(2);
    }

    @Test
    void indicadorLoteRechazaLoteInexistente() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.indicadorLote(loteId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.LOT_NOT_FOUND));
        verify(pesajes, never()).indicadorLote(anyUUID(), anyUUID());
    }

    @Test
    void indicadorLoteValidaAccesoALaPropiedad() {
        when(lotes.findById(loteId, company)).thenReturn(Optional.of(lote(UUID.randomUUID())));
        assertThatThrownBy(() -> service.indicadorLote(loteId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.PROPERTY_ACCESS_DENIED));
        verify(pesajes, never()).indicadorLote(anyUUID(), anyUUID());
    }

    @Test
    void animalesSinPesajeSeFiltranPorPropiedadesPermitidas() {
        when(pesajes.animalesSinPesaje(company, false, Set.of(property), 0, 20))
                .thenReturn(List.of(new PesajeSinPesaje(animalId, "A-1", "Animal 1",
                        LocalDate.of(2026, 6, 1), new BigDecimal("320"), 70)));

        List<PesajeSinPesaje> resultado = service.animalesSinPesaje(0, 20);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).diasSinPesaje()).isEqualTo(70);
        verify(pesajes, never()).animalesSinPesaje(company, true, Set.of(), 0, 20);
    }

    @Test
    void countAnimalesSinPesajeUsaLasPropiedadesDelUsuario() {
        when(pesajes.countAnimalesSinPesaje(company, false, Set.of(property))).thenReturn(3L);
        assertThat(service.countAnimalesSinPesaje()).isEqualTo(3L);
        verify(pesajes).countAnimalesSinPesaje(company, false, Set.of(property));
    }

    @Test
    void usuarioSinPermisoNoAccedeAIndicadores() {
        CurrentUser user = new CurrentUser(UUID.randomUUID(), company, UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(property), false);
        service = new PesajeIndicadorService(pesajes, animales, lotes, new UserContext(() -> user));
        assertThatThrownBy(() -> service.indicadorAnimal(animalId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.USER_NOT_AUTHORIZED));
        verify(animales, never()).findById(anyUUID(), anyUUID());
    }

    private static UUID anyUUID() {
        return org.mockito.ArgumentMatchers.any(UUID.class);
    }

    private Pesaje pesaje(LocalDate fecha, BigDecimal peso, TipoPesaje tipo) {
        return new Pesaje(UUID.randomUUID(), company, animalId, fecha, peso, tipo, null, null,
                UUID.randomUUID(), property, null, null, "WEB", UUID.randomUUID(), null,
                EstadoPesaje.ACTIVO, null, null, null, null, "A-1", "Animal 1", null, null, null, null, 0);
    }

    private Pesaje pesajeAnulado(LocalDate fecha, BigDecimal peso) {
        return new Pesaje(UUID.randomUUID(), company, animalId, fecha, peso, TipoPesaje.RUTINA, null, null,
                UUID.randomUUID(), property, null, null, "WEB", UUID.randomUUID(), null,
                EstadoPesaje.ANULADO, "Registro erróneo", UUID.randomUUID(), null, null,
                "A-1", "Animal 1", null, null, null, null, 1);
    }

    private Animal animal(UUID prop, UUID lote) {
        return new Animal(animalId, company, "A-1", "Animal 1", SexoAnimal.HEMBRA, null, false,
                UUID.randomUUID(), UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO,
                prop, null, lote, EstadoAnimal.ACTIVO, LocalDate.now(), null, null, null, null, null, 0);
    }

    private Lote lote(UUID prop) {
        return new Lote(loteId, company, prop, "L-1", "Lote 1", null, EstadoLote.ACTIVO,
                LocalDate.now(), null, 0);
    }
}
