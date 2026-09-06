package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class PlanSanitarioServiceTest {
    private SanidadRepository repo;
    private PlanSanitarioService service;
    private UUID empresa, plan, item;

    @BeforeEach
    void setUp() {
        repo = mock(SanidadRepository.class);
        empresa = UUID.randomUUID();
        plan = UUID.randomUUID();
        item = UUID.randomUUID();
        CurrentUser u = new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of(),
                Set.of("SANIDAD_VER", "SANIDAD_PLAN_ADMINISTRAR"), Set.of(), true);
        service = new PlanSanitarioService(repo, new UserContext(() -> u), mock(ApplicationEventPublisher.class),
                mock(EventoCalendarioSanitarioRepository.class));
        when(repo.plan(plan, empresa)).thenReturn(Optional.of(new PlanSanitario(plan, empresa, "Plan", null,
                LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0)));
    }

    @Test
    void calculaProximaAplicacionYAlertaEnSpring() {
        when(repo.items(plan, empresa, false)).thenReturn(List.of(new PlanSanitarioItem(item, empresa, plan,
                TipoActividadSanitaria.VACUNACION, null, "Vacuna", null, null, null, null, null, null, 180, 7, null,
                true, true, 0, OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO")));
        ProximaActividadSanitaria r = service.calcularProxima(plan, item, LocalDate.of(2026, 8, 13));
        assertThat(r.proximaAplicacion()).isEqualTo(LocalDate.of(2027, 2, 9));
        assertThat(r.fechaAlerta()).isEqualTo(LocalDate.of(2027, 2, 2));
    }

    @Test
    void nuevoPlanSiempreNaceEnBorrador() {
        when(repo.crearPlan(any(), any())).thenAnswer(i -> i.getArgument(0));
        PlanSanitario p = service.crearPlan(new CrearPlanSanitarioCommand("Bovinos", null, LocalDate.now(), null));
        assertThat(p.estado()).isEqualTo(EstadoPlanSanitario.BORRADOR);
    }

    // ---------- crearItem(): validaciones de modalidad/vía/lugar/dosis ----------

    @Test
    void creaUnaActividadPeriodicaValidaComoVersion1DeSuPropiaIdentidad() {
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        PlanSanitarioItem creado = service.crearItem(plan, comandoPeriodica(90));
        assertThat(creado.numeroVersion()).isEqualTo(1);
        assertThat(creado.identidadLogicaId()).isEqualTo(creado.id());
        assertThat(creado.versionAnteriorId()).isNull();
        assertThat(creado.vigenteHasta()).isNull();
    }

    @Test
    void rechazaPeriodicaSinFrecuenciaPositiva() {
        assertThatThrownBy(() -> service.crearItem(plan, comandoPeriodica(0)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_MODALIDAD_CONFIG_INVALIDA));
    }

    @Test
    void rechazaViaInyectableSinLugarAnatomico() {
        CrearPlanItemCommand c = conVia(ViaAdministracion.INTRAMUSCULAR, null);
        assertThatThrownBy(() -> service.crearItem(plan, c))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE));
    }

    @Test
    void rechazaViaOralConLugarIncompatible() {
        CrearPlanItemCommand c = conVia(ViaAdministracion.ORAL, LugarAplicacion.CUELLO);
        assertThatThrownBy(() -> service.crearItem(plan, c))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE));
    }

    @Test
    void permiteViaOralConBoca() {
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        CrearPlanItemCommand c = conVia(ViaAdministracion.ORAL, LugarAplicacion.BOCA);
        assertThat(service.crearItem(plan, c).viaAdministracionCodigo()).isEqualTo(ViaAdministracion.ORAL);
    }

    private CrearPlanItemCommand conVia(ViaAdministracion via, LugarAplicacion lugar) {
        CrearPlanItemCommand base = comandoBase(ModalidadActividad.MANUAL, new ModalidadConfig.ManualConfig());
        return new CrearPlanItemCommand(base.codigoInterno(), base.nombre(), base.descripcion(), base.tipoActividad(),
                base.modalidad(), base.modalidadConfig(), base.productoRecomendadoTexto(), base.principioActivo(),
                base.instruccionesVeterinario(), base.observaciones(), base.dosisCantidad(), base.dosisUnidad(),
                base.dosisUnidadDetalle(), base.dosisTipoCalculo(), base.dosisPesoReferenciaKg(), base.dosisMinima(),
                base.dosisMaxima(), via, null, lugar, null, base.categoriasAplicables(), base.sexoAplicable(),
                base.edadMinDias(), base.edadMaxDias(), base.edadUnidad(), base.permiteEdadDesconocida(),
                base.diasAlerta(), base.obligatorio(), base.origenRegulatorio(), base.especieAplicable(), null, null);
    }

    @Test
    void rechazaDosisPorPesoSinPesoDeReferencia() {
        CrearPlanItemCommand base = comandoBase(ModalidadActividad.MANUAL, new ModalidadConfig.ManualConfig());
        CrearPlanItemCommand c = new CrearPlanItemCommand(base.codigoInterno(), base.nombre(), base.descripcion(),
                base.tipoActividad(), base.modalidad(), base.modalidadConfig(), base.productoRecomendadoTexto(),
                base.principioActivo(), base.instruccionesVeterinario(), base.observaciones(),
                new BigDecimal("1"), UnidadDosis.ML, null, TipoCalculoDosis.POR_PESO, null, null, null,
                base.viaAdministracionCodigo(), null, base.lugarAplicacion(), null, base.categoriasAplicables(),
                base.sexoAplicable(), base.edadMinDias(), base.edadMaxDias(), base.edadUnidad(),
                base.permiteEdadDesconocida(), base.diasAlerta(), base.obligatorio(), base.origenRegulatorio(),
                base.especieAplicable(), null, null);
        assertThatThrownBy(() -> service.crearItem(plan, c))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    // ---------- actualizarItem(): versionado (sección 16-17) ----------

    @Test
    void editaDirectamenteUnaActividadNuncaUsada() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));
        when(repo.itemEnUso(item)).thenReturn(false);
        when(repo.actualizarItem(any(), any())).thenAnswer(i -> i.getArgument(0));

        PlanSanitarioItem resultado = service.actualizarItem(plan, item, comandoPeriodica(120), 0);

        verify(repo).actualizarItem(any(), any());
        verify(repo, never()).cerrarVigenciaItem(any(), any(), anyLong(), any());
        verify(repo, never()).crearItem(any(), any());
        assertThat(resultado.identidadLogicaId()).isEqualTo(actual.identidadLogicaId());
    }

    @Test
    void creaNuevaVersionAlEditarCampoVersionableDeUnaActividadEnUso() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));
        when(repo.itemEnUso(item)).thenReturn(true);
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        Instant vigencia = Instant.now();

        CrearPlanItemCommand cambioFrecuencia = comandoPeriodicaConVersion(120, "Cambio de protocolo", vigencia);
        PlanSanitarioItem nuevaVersion = service.actualizarItem(plan, item, cambioFrecuencia, 0);

        verify(repo).cerrarVigenciaItem(eq(item), eq(vigencia), eq(0L), any());
        verify(repo, never()).actualizarItem(any(), any());
        assertThat(nuevaVersion.numeroVersion()).isEqualTo(actual.numeroVersion() + 1);
        assertThat(nuevaVersion.versionAnteriorId()).isEqualTo(item);
        assertThat(nuevaVersion.identidadLogicaId()).isEqualTo(actual.identidadLogicaId());
    }

    @Test
    void exigeMotivoYVigenciaParaVersionarUnaActividadEnUso() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));
        when(repo.itemEnUso(item)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizarItem(plan, item, comandoPeriodica(120), 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_VERSION_MOTIVO_REQUERIDO));
    }

    @Test
    void permiteEditarDirectamenteUnCambioPuramenteAdministrativoAunqueEsteEnUso() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));
        when(repo.itemEnUso(item)).thenReturn(true);
        when(repo.actualizarItem(any(), any())).thenAnswer(i -> i.getArgument(0));

        // Mismo comando que generó `actual`, salvo observaciones (no es un campo "versionable").
        CrearPlanItemCommand base = comandoPeriodica(90);
        CrearPlanItemCommand soloObservaciones = new CrearPlanItemCommand(base.codigoInterno(), base.nombre(),
                base.descripcion(), base.tipoActividad(), base.modalidad(), base.modalidadConfig(),
                base.productoRecomendadoTexto(), base.principioActivo(), base.instruccionesVeterinario(),
                "Nota interna actualizada", base.dosisCantidad(), base.dosisUnidad(), base.dosisUnidadDetalle(),
                base.dosisTipoCalculo(), base.dosisPesoReferenciaKg(), base.dosisMinima(), base.dosisMaxima(),
                base.viaAdministracionCodigo(), base.viaAdministracionDetalle(), base.lugarAplicacion(),
                base.lugarAplicacionDetalle(), base.categoriasAplicables(), base.sexoAplicable(), base.edadMinDias(),
                base.edadMaxDias(), base.edadUnidad(), base.permiteEdadDesconocida(), base.diasAlerta(),
                base.obligatorio(), base.origenRegulatorio(), base.especieAplicable(), null, null);

        service.actualizarItem(plan, item, soloObservaciones, 0);

        verify(repo).actualizarItem(any(), any());
        verify(repo, never()).crearItem(any(), any());
    }

    private CrearPlanItemCommand comandoPeriodica(int frecuenciaDias) {
        return comandoBase(ModalidadActividad.PERIODICA,
                new ModalidadConfig.PeriodicaConfig(frecuenciaDias, UnidadFrecuencia.DIAS,
                        ReferenciaCalculoPeriodica.ULTIMA_APLICACION, 7, 0));
    }

    private CrearPlanItemCommand comandoPeriodicaConVersion(int frecuenciaDias, String motivo, Instant vigencia) {
        CrearPlanItemCommand base = comandoPeriodica(frecuenciaDias);
        return new CrearPlanItemCommand(base.codigoInterno(), base.nombre(), base.descripcion(), base.tipoActividad(),
                base.modalidad(), base.modalidadConfig(), base.productoRecomendadoTexto(), base.principioActivo(),
                base.instruccionesVeterinario(), base.observaciones(), base.dosisCantidad(), base.dosisUnidad(),
                base.dosisUnidadDetalle(), base.dosisTipoCalculo(), base.dosisPesoReferenciaKg(), base.dosisMinima(),
                base.dosisMaxima(), base.viaAdministracionCodigo(), base.viaAdministracionDetalle(),
                base.lugarAplicacion(), base.lugarAplicacionDetalle(), base.categoriasAplicables(),
                base.sexoAplicable(), base.edadMinDias(), base.edadMaxDias(), base.edadUnidad(),
                base.permiteEdadDesconocida(), base.diasAlerta(), base.obligatorio(), base.origenRegulatorio(),
                base.especieAplicable(), motivo, vigencia);
    }

    private CrearPlanItemCommand comandoBase(ModalidadActividad modalidad, ModalidadConfig config) {
        return new CrearPlanItemCommand(null, "Desparasitación trimestral", null,
                TipoActividadSanitaria.DESPARASITACION, modalidad, config, "Ivermectina 1%", "Ivermectina", null,
                null, null, null, null, TipoCalculoDosis.NO_APLICA, null, null, null, null, null, null, null,
                List.of(), null, null, null, UnidadEdadActividad.DIAS, false, 5, false,
                OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO", null, null);
    }

    private PlanSanitarioItem itemPeriodica(int frecuenciaDias, boolean enUso) {
        Instant ahora = Instant.now();
        return new PlanSanitarioItem(item, empresa, plan, TipoActividadSanitaria.DESPARASITACION, null,
                "Ivermectina 1%", null, null, null, null, null, null, frecuenciaDias, 5, null, false, true, 0,
                OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO", false, item, 1, null, ahora, null,
                null, null, "Desparasitación trimestral", null, "Ivermectina", null, null, null, null, null,
                TipoCalculoDosis.NO_APLICA, null, null, null, null, null, null, null, List.of(),
                UnidadEdadActividad.DIAS, ModalidadActividad.PERIODICA,
                new ModalidadConfig.PeriodicaConfig(frecuenciaDias, UnidadFrecuencia.DIAS,
                        ReferenciaCalculoPeriodica.ULTIMA_APLICACION, 7, 0), false);
    }
}
