package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.codigos.CodigoService;
import bo.com.ganadero.shared.codigos.TipoCodigo;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
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
    private CodigoService codigos;
    private PlanSanitarioService service;
    private EventoCalendarioSanitarioRepository eventos;
    private MotorAlertas motor;
    private ApplicationEventPublisher publicador;
    private UUID empresa, plan, item;

    @BeforeEach
    void setUp() {
        repo = mock(SanidadRepository.class);
        codigos = mock(CodigoService.class);
        empresa = UUID.randomUUID();
        plan = UUID.randomUUID();
        item = UUID.randomUUID();
        CurrentUser u = new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of(),
                Set.of("SANIDAD_VER", "SANIDAD_PLAN_ADMINISTRAR"), Set.of(), true);
        eventos = mock(EventoCalendarioSanitarioRepository.class);
        motor = mock(MotorAlertas.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<MotorAlertas> alertas = mock(ObjectProvider.class);
        when(alertas.getIfAvailable()).thenReturn(motor);
        publicador = mock(ApplicationEventPublisher.class);
        service = new PlanSanitarioService(repo, new UserContext(() -> u), publicador,
                eventos, codigos, alertas);
        when(repo.plan(plan, empresa)).thenReturn(Optional.of(new PlanSanitario(plan, empresa, "Plan", null,
                LocalDate.now(), null, EstadoPlanSanitario.ACTIVO, null, null, 0)));
    }

    @Test
    void generaElCodigoDeUnaEnfermedadNuevaEnVezDeExigirloEnElFormulario() {
        when(codigos.paraCreacion(any(), eq(TipoCodigo.ENFERMEDAD), eq(null), eq(null), eq(null)))
                .thenReturn("ENF-001");
        when(repo.crearEnfermedad(any())).thenAnswer(i -> i.getArgument(0));

        Enfermedad creada = service.crearEnfermedad(new CrearEnfermedadCommand(null, "Fiebre aftosa", null, true));

        assertThat(creada.codigo()).isEqualTo("ENF-001");
        verify(codigos).paraCreacion(any(), eq(TipoCodigo.ENFERMEDAD), eq(null), eq(null), eq(null));
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
    void rechazaLugarOtroSinDetalleAunqueNoHayaVia() {
        CrearPlanItemCommand c = conVia(null, LugarAplicacion.OTRO);
        assertThatThrownBy(() -> service.crearItem(plan, c))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_VIA_LUGAR_INCOMPATIBLE);
                    assertThat(e.getMessage()).isEqualTo("Indica el detalle del lugar.");
                });
    }

    @Test
    void permiteViaOralConBoca() {
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        CrearPlanItemCommand c = conVia(ViaAdministracion.ORAL, LugarAplicacion.BOCA);
        assertThat(service.crearItem(plan, c).viaAdministracionCodigo()).isEqualTo(ViaAdministracion.ORAL);
    }

    // ---------- planes: fechas, estado y efecto en el calendario ----------

    @Test
    void rechazaUnPlanConFechaDeFinAnteriorALaDeInicioConUnMensajeClaro() {
        CrearPlanSanitarioCommand c = new CrearPlanSanitarioCommand("Plan", null, LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 5, 31), null);
        assertThatThrownBy(() -> service.crearPlan(c))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(e.getMessage()).isEqualTo("La fecha de fin no puede ser anterior a la fecha de inicio.");
                });
        verify(repo, never()).crearPlan(any(), any());
    }

    @Test
    void unPlanFinalizadoOAnuladoYaNoAdmiteCrearEditarNiCambiarElEstadoDeSusActividades() {
        for (EstadoPlanSanitario cerrado : List.of(EstadoPlanSanitario.FINALIZADO, EstadoPlanSanitario.ANULADO)) {
            planEn(cerrado);
            assertThatThrownBy(() -> service.crearItem(plan, comandoPeriodica(90)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_PLAN_CERRADO));
            assertThatThrownBy(() -> service.actualizarItem(plan, item, comandoPeriodica(90), 0))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_PLAN_CERRADO));
            assertThatThrownBy(() -> service.estadoItem(plan, item, false, 0))
                    .isInstanceOfSatisfying(BusinessException.class,
                            e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_PLAN_CERRADO));
        }
        verify(repo, never()).crearItem(any(), any());
        verify(repo, never()).actualizarItem(any(), any());
        verify(repo, never()).cambiarEstadoItem(any(), any(), any(), org.mockito.ArgumentMatchers.anyBoolean(), anyLong(), any());
    }

    @Test
    void unPlanEnBorradorSiAdmiteActividadesParaIrPreparandolo() {
        planEn(EstadoPlanSanitario.BORRADOR);
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.crearItem(plan, comandoPeriodica(90))).isNotNull();
    }

    @Test
    void alFinalizarOAnularUnPlanCancelaSusPendientesYResuelveSoloLasAlertasDeOcurrenciasVacias() {
        UUID vacia = UUID.randomUUID(), conPendientes = UUID.randomUUID();
        when(eventos.cancelarPendientesDePlan(plan)).thenReturn(List.of(vacia, conPendientes));
        when(eventos.tienePendientes(vacia)).thenReturn(false);
        when(eventos.tienePendientes(conPendientes)).thenReturn(true);
        planEn(EstadoPlanSanitario.ACTIVO);

        service.cambiarEstado(plan, EstadoPlanSanitario.FINALIZADO, 0);

        verify(eventos).cancelarPendientesDePlan(plan);
        verify(motor).resolverPorOrigen(empresa, "EVENTO_CALENDARIO_SANITARIO", vacia);
        verify(motor, never()).resolverPorOrigen(empresa, "EVENTO_CALENDARIO_SANITARIO", conPendientes);

        planEn(EstadoPlanSanitario.BORRADOR);
        service.cambiarEstado(plan, EstadoPlanSanitario.ANULADO, 0);
        verify(eventos, org.mockito.Mockito.times(2)).cancelarPendientesDePlan(plan);
    }

    @Test
    void alActivarUnPlanNoSeCancelaNada() {
        planEn(EstadoPlanSanitario.BORRADOR);

        service.cambiarEstado(plan, EstadoPlanSanitario.ACTIVO, 0);

        verify(eventos, never()).cancelarPendientesDePlan(any());
    }

    @Test
    void alDesactivarUnaActividadCancelaSusPendientesYResuelveLasAlertasVacias() {
        UUID vacia = UUID.randomUUID();
        when(repo.cambiarEstadoItem(eq(item), eq(plan), eq(empresa), eq(false), eq(0L), any()))
                .thenReturn(itemCon(null, null, null, TipoCalculoDosis.NO_APLICA, null, false));
        when(eventos.cancelarPendientesDeActividad(item)).thenReturn(List.of(vacia));
        when(eventos.tienePendientes(vacia)).thenReturn(false);

        service.estadoItem(plan, item, false, 0);

        verify(eventos).cancelarPendientesDeActividad(item);
        verify(motor).resolverPorOrigen(empresa, "EVENTO_CALENDARIO_SANITARIO", vacia);
        verify(eventos, never()).restaurarCanceladosFuturos(any());
    }

    @Test
    void alReactivarUnaActividadVigenteRestauraLosCanceladosQueAunNoVencieron() {
        when(repo.cambiarEstadoItem(eq(item), eq(plan), eq(empresa), eq(true), eq(0L), any()))
                .thenReturn(itemCon(null, null, null, TipoCalculoDosis.NO_APLICA, null, false));

        service.estadoItem(plan, item, true, 0);

        verify(eventos).restaurarCanceladosFuturos(item);
        verify(eventos, never()).cancelarPendientesDeActividad(any());
    }

    @Test
    void reactivarUnaVersionYaCerradaNoResucitaSusEventos() {
        when(repo.cambiarEstadoItem(eq(item), eq(plan), eq(empresa), eq(true), eq(0L), any()))
                .thenReturn(itemCon(Instant.now(), null, null, TipoCalculoDosis.NO_APLICA, null, false));

        service.estadoItem(plan, item, true, 0);

        verify(eventos, never()).restaurarCanceladosFuturos(any());
    }

    // ---------- versionado: qué campos cuentan como cambio de la actividad ----------

    @Test
    void cambiarElPesoDeReferenciaDeUnaActividadEnUsoExigeUnaNuevaVersion() {
        // El peso de referencia cambia la dosis que se calcula: no puede editarse en silencio.
        when(repo.item(item, empresa)).thenReturn(Optional.of(
                itemCon(null, new BigDecimal("1"), UnidadDosis.ML, TipoCalculoDosis.POR_PESO, new BigDecimal("50"), false)));
        when(repo.itemEnUso(item)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizarItem(plan, item,
                comandoPorPeso(new BigDecimal("100")), 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_VERSION_MOTIVO_REQUERIDO));
        verify(repo, never()).actualizarItem(any(), any());
    }

    @Test
    void elMismoPesoConOtraEscalaNoGeneraUnaVersionFalsa() {
        when(repo.item(item, empresa)).thenReturn(Optional.of(
                itemCon(null, new BigDecimal("1.000"), UnidadDosis.ML, TipoCalculoDosis.POR_PESO, new BigDecimal("50.0"), false)));
        when(repo.itemEnUso(item)).thenReturn(true);
        when(repo.actualizarItem(any(), any())).thenAnswer(i -> i.getArgument(0));

        service.actualizarItem(plan, item, comandoPorPeso(new BigDecimal("50")), 0);

        verify(repo).actualizarItem(any(), any());
        verify(repo, never()).crearItem(any(), any());
    }

    @Test
    void cambiarLaPoliticaDeEdadDesconocidaDeUnaActividadEnUsoExigeUnaNuevaVersion() {
        // Cambia a quién se le programa la actividad.
        when(repo.item(item, empresa)).thenReturn(Optional.of(
                itemCon(null, null, null, TipoCalculoDosis.NO_APLICA, null, false)));
        when(repo.itemEnUso(item)).thenReturn(true);
        CrearPlanItemCommand base = comandoPeriodica(90);
        CrearPlanItemCommand permite = new CrearPlanItemCommand(base.codigoInterno(), base.nombre(), base.descripcion(),
                base.tipoActividad(), base.modalidad(), base.modalidadConfig(), base.productoRecomendadoTexto(),
                base.principioActivo(), base.instruccionesVeterinario(), base.observaciones(), base.dosisCantidad(),
                base.dosisUnidad(), base.dosisUnidadDetalle(), base.dosisTipoCalculo(), base.dosisPesoReferenciaKg(),
                base.dosisMinima(), base.dosisMaxima(), base.viaAdministracionCodigo(), base.viaAdministracionDetalle(),
                base.lugarAplicacion(), base.lugarAplicacionDetalle(), base.categoriasAplicables(), base.sexoAplicable(),
                base.edadMinDias(), base.edadMaxDias(), base.edadUnidad(), true, base.diasAlerta(), base.obligatorio(),
                base.origenRegulatorio(), base.especieAplicable(), null, null);

        assertThatThrownBy(() -> service.actualizarItem(plan, item, permite, 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_VERSION_MOTIVO_REQUERIDO));
    }

    private void planEn(EstadoPlanSanitario estado) {
        when(repo.plan(plan, empresa)).thenReturn(Optional.of(new PlanSanitario(plan, empresa, "Plan", null,
                LocalDate.now(), null, estado, null, null, 0)));
    }

    /** Actividad periódica vigente (o cerrada si se da {@code vigenteHasta}) con la dosis indicada. */
    private PlanSanitarioItem itemCon(Instant vigenteHasta, BigDecimal cantidad, UnidadDosis unidad,
                                      TipoCalculoDosis tipo, BigDecimal pesoReferencia, boolean permiteEdadDesconocida) {
        Instant ahora = Instant.now();
        return new PlanSanitarioItem(item, empresa, plan, TipoActividadSanitaria.DESPARASITACION, null,
                "Ivermectina 1%", null, null, null, null, null, null, 90, 5, null, false, true, 0,
                OrigenRegulatorioActividad.CONFIGURABLE_ESTABLECIMIENTO, "BOVINO", permiteEdadDesconocida, item, 1, null,
                ahora, vigenteHasta, null, null, "Desparasitación trimestral", null, "Ivermectina", null, null,
                cantidad, unidad, null, tipo, pesoReferencia, null, null, null, null, null, null, List.of(),
                UnidadEdadActividad.DIAS, ModalidadActividad.PERIODICA,
                new ModalidadConfig.PeriodicaConfig(90, UnidadFrecuencia.DIAS,
                        ReferenciaCalculoPeriodica.ULTIMA_APLICACION, 7, 0), false);
    }

    private CrearPlanItemCommand comandoPorPeso(BigDecimal pesoReferencia) {
        CrearPlanItemCommand base = comandoPeriodica(90);
        return new CrearPlanItemCommand(base.codigoInterno(), base.nombre(), base.descripcion(), base.tipoActividad(),
                base.modalidad(), base.modalidadConfig(), base.productoRecomendadoTexto(), base.principioActivo(),
                base.instruccionesVeterinario(), base.observaciones(), new BigDecimal("1"), UnidadDosis.ML,
                base.dosisUnidadDetalle(), TipoCalculoDosis.POR_PESO, pesoReferencia, base.dosisMinima(),
                base.dosisMaxima(), base.viaAdministracionCodigo(), base.viaAdministracionDetalle(),
                base.lugarAplicacion(), base.lugarAplicacionDetalle(), base.categoriasAplicables(),
                base.sexoAplicable(), base.edadMinDias(), base.edadMaxDias(), base.edadUnidad(),
                base.permiteEdadDesconocida(), base.diasAlerta(), base.obligatorio(), base.origenRegulatorio(),
                base.especieAplicable(), null, null);
    }

    // ---------- POR_EDAD: la edad objetivo debe caer dentro del rango de animales elegibles ----------

    @Test
    void rechazaEdadObjetivoMayorQueLaEdadMaximaDeLosElegibles() {
        // 7 meses = 210 días > máximo de 180 días: ningún animal llegaría a ser elegible.
        CrearPlanItemCommand c = comandoPorEdad(7, UnidadEdadActividad.MESES, null, 180);
        assertThatThrownBy(() -> service.crearItem(plan, c))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_ITEM_EDAD_INVALIDA);
                    assertThat(e.getMessage()).contains("210").contains("supera").contains("180");
                });
        verify(repo, never()).crearItem(any(), any());
    }

    @Test
    void rechazaEdadObjetivoMenorQueLaEdadMinimaDeLosElegibles() {
        CrearPlanItemCommand c = comandoPorEdad(3, UnidadEdadActividad.MESES, 120, null);
        assertThatThrownBy(() -> service.crearItem(plan, c))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_ITEM_EDAD_INVALIDA);
                    assertThat(e.getMessage()).contains("90").contains("menor").contains("120");
                });
    }

    @Test
    void conviertePorLaUnidadAntesDeComparar() {
        // 1 año = 365 días, que supera un máximo de 360 días; 12 meses = 360 días, que no.
        assertThatThrownBy(() -> service.crearItem(plan, comandoPorEdad(1, UnidadEdadActividad.ANIOS, null, 360)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_ITEM_EDAD_INVALIDA));
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.crearItem(plan, comandoPorEdad(12, UnidadEdadActividad.MESES, null, 360))).isNotNull();
    }

    @Test
    void aceptaEdadObjetivoDentroDelRangoIncluidosLosLimitesYSinRango() {
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.crearItem(plan, comandoPorEdad(7, UnidadEdadActividad.MESES, 210, 210))).isNotNull();
        assertThat(service.crearItem(plan, comandoPorEdad(7, UnidadEdadActividad.MESES, 90, 240))).isNotNull();
        assertThat(service.crearItem(plan, comandoPorEdad(7, UnidadEdadActividad.MESES, null, null))).isNotNull();
    }

    @Test
    void tambienValidaAlEditarUnaActividadPorEdad() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));

        assertThatThrownBy(() -> service.actualizarItem(plan, item,
                comandoPorEdad(7, UnidadEdadActividad.MESES, null, 180), 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_ITEM_EDAD_INVALIDA));
        verify(repo, never()).actualizarItem(any(), any());
    }

    private CrearPlanItemCommand comandoPorEdad(int edadObjetivo, UnidadEdadActividad unidad, Integer edadMinDias,
                                                Integer edadMaxDias) {
        CrearPlanItemCommand base = comandoBase(ModalidadActividad.POR_EDAD, new ModalidadConfig.PorEdadConfig(
                edadObjetivo, unidad, 5, 15, PoliticaEdadEstimada.PERMITIR, PoliticaEdadDesconocida.EXCLUIR, true));
        return new CrearPlanItemCommand(base.codigoInterno(), base.nombre(), base.descripcion(), base.tipoActividad(),
                base.modalidad(), base.modalidadConfig(), base.productoRecomendadoTexto(), base.principioActivo(),
                base.instruccionesVeterinario(), base.observaciones(), base.dosisCantidad(), base.dosisUnidad(),
                base.dosisUnidadDetalle(), base.dosisTipoCalculo(), base.dosisPesoReferenciaKg(), base.dosisMinima(),
                base.dosisMaxima(), base.viaAdministracionCodigo(), base.viaAdministracionDetalle(),
                base.lugarAplicacion(), base.lugarAplicacionDetalle(), base.categoriasAplicables(),
                base.sexoAplicable(), edadMinDias, edadMaxDias, base.edadUnidad(), base.permiteEdadDesconocida(),
                base.diasAlerta(), base.obligatorio(), base.origenRegulatorio(), base.especieAplicable(), null, null);
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

    // ---------- el calendario se genera al cambiar el plan, sin esperar a la corrida nocturna ----------

    private void verificarQuePidioGenerarElCalendario(int veces) {
        verify(publicador, org.mockito.Mockito.times(veces)).publishEvent(any(PlanSanitarioModificado.class));
    }

    @Test
    void alCrearUnaActividadPideGenerarElCalendario() {
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));

        service.crearItem(plan, comandoPeriodica(90));

        verificarQuePidioGenerarElCalendario(1);
    }

    @Test
    void alEditarUnaActividadDeLaMismaFilaOCreandoVersionPideGenerarElCalendario() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));
        when(repo.actualizarItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));

        when(repo.itemEnUso(item)).thenReturn(false);
        service.actualizarItem(plan, item, comandoPeriodica(120), 0);
        verificarQuePidioGenerarElCalendario(1);

        when(repo.itemEnUso(item)).thenReturn(true);
        service.actualizarItem(plan, item, comandoPeriodicaConVersion(120, "Cambio de protocolo", Instant.now()), 0);
        verificarQuePidioGenerarElCalendario(2);
    }

    @Test
    void alReactivarUnaActividadPideGenerarElCalendarioPeroAlDesactivarlaNo() {
        when(repo.cambiarEstadoItem(eq(item), eq(plan), eq(empresa), eq(true), eq(0L), any()))
                .thenReturn(itemCon(null, null, null, TipoCalculoDosis.NO_APLICA, null, false));
        when(repo.cambiarEstadoItem(eq(item), eq(plan), eq(empresa), eq(false), eq(0L), any()))
                .thenReturn(itemCon(null, null, null, TipoCalculoDosis.NO_APLICA, null, false));

        service.estadoItem(plan, item, false, 0);
        verificarQuePidioGenerarElCalendario(0);

        service.estadoItem(plan, item, true, 0);
        verificarQuePidioGenerarElCalendario(1);
    }

    @Test
    void alActivarUnPlanPideGenerarElCalendarioPeroAlFinalizarloNo() {
        planEn(EstadoPlanSanitario.ACTIVO);
        service.cambiarEstado(plan, EstadoPlanSanitario.FINALIZADO, 0);
        verificarQuePidioGenerarElCalendario(0);

        planEn(EstadoPlanSanitario.BORRADOR);
        service.cambiarEstado(plan, EstadoPlanSanitario.ACTIVO, 0);
        verificarQuePidioGenerarElCalendario(1);
    }

    @Test
    void unaActividadRechazadaPorValidacionNoPideGenerarElCalendario() {
        assertThatThrownBy(() -> service.crearItem(plan, comandoPeriodica(0))).isInstanceOf(BusinessException.class);

        verificarQuePidioGenerarElCalendario(0);
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
        verify(eventos, never()).cancelarPendientesDeActividad(any());
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
    void alCrearUnaVersionCancelaLosPendientesDeLaAnteriorYResuelveSoloLasAlertasDeOcurrenciasSinPendientes() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));
        when(repo.itemEnUso(item)).thenReturn(true);
        when(repo.crearItem(any(), any())).thenAnswer(i -> i.getArgument(0));
        UUID vacia = UUID.randomUUID(), conPendientes = UUID.randomUUID();
        when(eventos.cancelarPendientesDeActividad(item)).thenReturn(List.of(vacia, conPendientes));
        when(eventos.tienePendientes(vacia)).thenReturn(false);
        when(eventos.tienePendientes(conPendientes)).thenReturn(true);

        service.actualizarItem(plan, item, comandoPeriodicaConVersion(120, "Cambio de protocolo", Instant.now()), 0);

        verify(eventos).cancelarPendientesDeActividad(item);
        verify(motor).resolverPorOrigen(empresa, "EVENTO_CALENDARIO_SANITARIO", vacia);
        verify(motor, never()).resolverPorOrigen(empresa, "EVENTO_CALENDARIO_SANITARIO", conPendientes);
    }

    @Test
    void exigeMotivoYVigenciaParaVersionarUnaActividadEnUso() {
        PlanSanitarioItem actual = itemPeriodica(90, false);
        when(repo.item(item, empresa)).thenReturn(Optional.of(actual));
        when(repo.itemEnUso(item)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizarItem(plan, item, comandoPeriodica(120), 0))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SANIDAD_VERSION_MOTIVO_REQUERIDO));
        verify(eventos, never()).cancelarPendientesDeActividad(any());
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
