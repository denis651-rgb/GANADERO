package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.domain.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/**
 * Datos de una actividad sanitaria (sección 2). {@code motivoVersion}/{@code fechaVigencia} sólo
 * se exigen cuando se edita una actividad ya usada (sección 16-17); se ignoran al crear una nueva.
 */
public record CrearPlanItemCommand(
        String codigoInterno, String nombre, String descripcion, TipoActividadSanitaria tipoActividad,
        ModalidadActividad modalidad, ModalidadConfig modalidadConfig, String productoRecomendadoTexto,
        String principioActivo, String instruccionesVeterinario, String observaciones,
        BigDecimal dosisCantidad, UnidadDosis dosisUnidad, String dosisUnidadDetalle, TipoCalculoDosis dosisTipoCalculo,
        BigDecimal dosisPesoReferenciaKg, BigDecimal dosisMinima, BigDecimal dosisMaxima,
        ViaAdministracion viaAdministracionCodigo, String viaAdministracionDetalle, LugarAplicacion lugarAplicacion,
        String lugarAplicacionDetalle, List<UUID> categoriasAplicables, SexoAnimal sexoAplicable,
        Integer edadMinDias, Integer edadMaxDias, UnidadEdadActividad edadUnidad, boolean permiteEdadDesconocida,
        int diasAlerta, boolean obligatorio, OrigenRegulatorioActividad origenRegulatorio, String especieAplicable,
        String motivoVersion, Instant fechaVigencia, LocalTime horaEjecucion, List<LocalTime> horariosAviso) {

    /** Compatibilidad para llamadas internas anteriores a la programación horaria de la fase 1. */
    public CrearPlanItemCommand(
            String codigoInterno, String nombre, String descripcion, TipoActividadSanitaria tipoActividad,
            ModalidadActividad modalidad, ModalidadConfig modalidadConfig, String productoRecomendadoTexto,
            String principioActivo, String instruccionesVeterinario, String observaciones,
            BigDecimal dosisCantidad, UnidadDosis dosisUnidad, String dosisUnidadDetalle,
            TipoCalculoDosis dosisTipoCalculo, BigDecimal dosisPesoReferenciaKg, BigDecimal dosisMinima,
            BigDecimal dosisMaxima, ViaAdministracion viaAdministracionCodigo, String viaAdministracionDetalle,
            LugarAplicacion lugarAplicacion, String lugarAplicacionDetalle, List<UUID> categoriasAplicables,
            SexoAnimal sexoAplicable, Integer edadMinDias, Integer edadMaxDias, UnidadEdadActividad edadUnidad,
            boolean permiteEdadDesconocida, int diasAlerta, boolean obligatorio,
            OrigenRegulatorioActividad origenRegulatorio, String especieAplicable, String motivoVersion,
            Instant fechaVigencia) {
        this(codigoInterno, nombre, descripcion, tipoActividad, modalidad, modalidadConfig,
                productoRecomendadoTexto, principioActivo, instruccionesVeterinario, observaciones,
                dosisCantidad, dosisUnidad, dosisUnidadDetalle, dosisTipoCalculo, dosisPesoReferenciaKg,
                dosisMinima, dosisMaxima, viaAdministracionCodigo, viaAdministracionDetalle, lugarAplicacion,
                lugarAplicacionDetalle, categoriasAplicables, sexoAplicable, edadMinDias, edadMaxDias,
                edadUnidad, permiteEdadDesconocida, diasAlerta, obligatorio, origenRegulatorio,
                especieAplicable, motivoVersion, fechaVigencia, LocalTime.of(8, 0), List.of(LocalTime.of(8, 0)));
    }
}
