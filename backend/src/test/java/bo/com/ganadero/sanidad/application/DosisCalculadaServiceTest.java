package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.pesajes.domain.*;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DosisCalculadaServiceTest {
    private PesajeRepository pesajes;
    private DosisCalculadaService service;
    private UUID empresa, animalId;

    @BeforeEach
    void setUp() {
        pesajes = mock(PesajeRepository.class);
        service = new DosisCalculadaService(pesajes);
        empresa = UUID.randomUUID();
        animalId = UUID.randomUUID();
    }

    private PlanSanitarioItem actividadPorPeso(BigDecimal cantidad, BigDecimal referenciaKg, BigDecimal min, BigDecimal max) {
        return new PlanSanitarioItem(UUID.randomUUID(), empresa, UUID.randomUUID(), TipoActividadSanitaria.DESPARASITACION,
                null, "Ivermectina 1%", null, null, null, null, null, null, null, 0, null, false, true, 0,
                OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO", false, UUID.randomUUID(), 1, null,
                Instant.now(), null, null, null, "Desparasitación", null, "Ivermectina", null, null, cantidad,
                UnidadDosis.ML, null, TipoCalculoDosis.POR_PESO, referenciaKg, min, max, ViaAdministracion.POUR_ON,
                null, LugarAplicacion.LINEA_DORSAL, null, List.of(), UnidadEdadActividad.DIAS,
                ModalidadActividad.MANUAL, new ModalidadConfig.ManualConfig(), false);
    }

    private Pesaje pesaje(BigDecimal kg, TipoPeso tipo, LocalDate fecha) {
        return new Pesaje(UUID.randomUUID(), empresa, animalId, fecha, kg, TipoPesaje.RUTINA, tipo, null, null, null,
                null, null, null, null, null, null, null, null, null, EstadoPesaje.ACTIVO, null, null, null, null,
                null, null, null, null, null, null, 0);
    }

    @Test
    void calculaLaDosisProporcionalAlUltimoPesoMedido() {
        when(pesajes.findByAnimal(animalId, empresa)).thenReturn(List.of(
                pesaje(new BigDecimal("100"), TipoPeso.MEDIDO, LocalDate.now())));
        CalculoDosis r = service.calcular(actividadPorPeso(new BigDecimal("1"), new BigDecimal("50"), null, null), animalId, empresa);
        assertThat(r.dosisCalculada()).isEqualByComparingTo("2.000");
        assertThat(r.pesoTipo()).isEqualTo(TipoPeso.MEDIDO);
    }

    @Test
    void usaElUltimoPesoEstimadoCuandoNoHayNingunMedidoYLoMarcaComoTal() {
        when(pesajes.findByAnimal(animalId, empresa)).thenReturn(List.of(
                pesaje(new BigDecimal("75"), TipoPeso.ESTIMADO, LocalDate.now())));
        CalculoDosis r = service.calcular(actividadPorPeso(new BigDecimal("1"), new BigDecimal("50"), null, null), animalId, empresa);
        assertThat(r.dosisCalculada()).isEqualByComparingTo("1.500");
        assertThat(r.pesoTipo()).isEqualTo(TipoPeso.ESTIMADO);
    }

    @Test
    void prefiereElPesoMedidoAunSiElEstimadoEsMasReciente() {
        when(pesajes.findByAnimal(animalId, empresa)).thenReturn(List.of(
                pesaje(new BigDecimal("80"), TipoPeso.ESTIMADO, LocalDate.now()),
                pesaje(new BigDecimal("100"), TipoPeso.MEDIDO, LocalDate.now().minusDays(10))));
        CalculoDosis r = service.calcular(actividadPorPeso(new BigDecimal("1"), new BigDecimal("50"), null, null), animalId, empresa);
        assertThat(r.pesoTipo()).isEqualTo(TipoPeso.MEDIDO);
        assertThat(r.pesoUsadoKg()).isEqualByComparingTo("100");
    }

    @Test
    void fallaSiElAnimalNoTieneNingunPesoRegistrado() {
        when(pesajes.findByAnimal(animalId, empresa)).thenReturn(List.of());
        assertThatThrownBy(() -> service.calcular(actividadPorPeso(new BigDecimal("1"), new BigDecimal("50"), null, null), animalId, empresa))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_PESO_REQUERIDO_PARA_DOSIS));
    }

    @Test
    void aplicaElClampDeDosisMaximaConfigurada() {
        when(pesajes.findByAnimal(animalId, empresa)).thenReturn(List.of(
                pesaje(new BigDecimal("500"), TipoPeso.MEDIDO, LocalDate.now())));
        CalculoDosis r = service.calcular(
                actividadPorPeso(new BigDecimal("1"), new BigDecimal("50"), null, new BigDecimal("6")), animalId, empresa);
        assertThat(r.dosisCalculada()).isEqualByComparingTo("6");
    }

    @Test
    void ignoraPesajesAnuladosAlBuscarElUltimoValido() {
        Pesaje anulado = new Pesaje(UUID.randomUUID(), empresa, animalId, LocalDate.now(), new BigDecimal("120"),
                TipoPesaje.RUTINA, TipoPeso.MEDIDO, null, null, null, null, null, null, null, null, null, null, null,
                null, EstadoPesaje.ANULADO, "error", null, null, null, null, null, null, null, null, null, 0);
        when(pesajes.findByAnimal(animalId, empresa)).thenReturn(List.of(anulado,
                pesaje(new BigDecimal("100"), TipoPeso.MEDIDO, LocalDate.now().minusDays(1))));
        CalculoDosis r = service.calcular(actividadPorPeso(new BigDecimal("1"), new BigDecimal("50"), null, null), animalId, empresa);
        assertThat(r.pesoUsadoKg()).isEqualByComparingTo("100");
    }

    @Test
    void sinTipoDeCalculoPorPesoDevuelveDirectamenteLaCantidadConfiguradaSinConsultarPesos() {
        PlanSanitarioItem fija = new PlanSanitarioItem(UUID.randomUUID(), empresa, UUID.randomUUID(),
                TipoActividadSanitaria.VITAMINIZACION, null, "Complejo B", null, null, null, null, null, null, null,
                0, null, false, true, 0, OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO", false,
                UUID.randomUUID(), 1, null, Instant.now(), null, null, null, "Vitaminización", null, null, null, null,
                new BigDecimal("5"), UnidadDosis.ML, null, TipoCalculoDosis.FIJA_POR_ANIMAL, null, null, null,
                ViaAdministracion.SUBCUTANEA, null, LugarAplicacion.TABLA_DEL_CUELLO, null, List.of(),
                UnidadEdadActividad.DIAS, ModalidadActividad.MANUAL, new ModalidadConfig.ManualConfig(), false);
        CalculoDosis r = service.calcular(fija, animalId, empresa);
        assertThat(r.dosisCalculada()).isEqualByComparingTo("5");
        assertThat(r.pesoUsadoKg()).isNull();
    }
}
