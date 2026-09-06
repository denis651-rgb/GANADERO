package bo.com.ganadero.sanidad.domain;

import bo.com.ganadero.animales.domain.SexoAnimal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Una versión de una actividad sanitaria (sección 2). {@code identidadLogicaId} es constante a
 * través de todas las versiones de la misma actividad; {@code id} identifica esta versión
 * concreta. {@code vigenteHasta==null} significa que esta es la versión actualmente vigente.
 */
public record PlanSanitarioItem(
        UUID id, UUID empresaId, UUID planId, TipoActividadSanitaria tipoActividad, UUID productoId,
        String productoRecomendadoTexto, UUID categoriaAnimalId, SexoAnimal sexoAplicable, Integer edadMinDias,
        Integer edadMaxDias, BigDecimal dosis, String unidadDosis, Integer frecuenciaDias, int diasAlerta,
        String viaAdministracion, boolean obligatorio, boolean activo, long version,
        OrigenRegulatorioActividad origenRegulatorio, String especieAplicable, boolean permiteEdadDesconocida,
        UUID identidadLogicaId, int numeroVersion, UUID versionAnteriorId, Instant vigenteDesde,
        Instant vigenteHasta, String motivoVersion, String codigoInterno, String nombre, String descripcion,
        String principioActivo, String instruccionesVeterinario, String observaciones, BigDecimal dosisCantidad,
        UnidadDosis dosisUnidad, String dosisUnidadDetalle, TipoCalculoDosis dosisTipoCalculo,
        BigDecimal dosisPesoReferenciaKg, BigDecimal dosisMinima, BigDecimal dosisMaxima,
        ViaAdministracion viaAdministracionCodigo, String viaAdministracionDetalle, LugarAplicacion lugarAplicacion,
        String lugarAplicacionDetalle, List<UUID> categoriasAplicables, UnidadEdadActividad edadUnidad,
        ModalidadActividad modalidad, ModalidadConfig modalidadConfig, boolean requiereRevision) {

    /** Constructor legado (pre-versionado/pre-dosis-estructurada): usado por tests existentes. */
    public PlanSanitarioItem(UUID id, UUID empresaId, UUID planId, TipoActividadSanitaria tipoActividad,
                             UUID productoId, String productoRecomendadoTexto, UUID categoriaAnimalId,
                             SexoAnimal sexoAplicable, Integer edadMinDias, Integer edadMaxDias, BigDecimal dosis,
                             String unidadDosis, Integer frecuenciaDias, int diasAlerta, String viaAdministracion,
                             boolean obligatorio, boolean activo, long version,
                             OrigenRegulatorioActividad origenRegulatorio, String especieAplicable,
                             boolean permiteEdadDesconocida) {
        this(id, empresaId, planId, tipoActividad, productoId, productoRecomendadoTexto, categoriaAnimalId,
                sexoAplicable, edadMinDias, edadMaxDias, dosis, unidadDosis, frecuenciaDias, diasAlerta,
                viaAdministracion, obligatorio, activo, version, origenRegulatorio, especieAplicable,
                permiteEdadDesconocida,
                id, 1, null, Instant.EPOCH, null, null, null,
                productoRecomendadoTexto != null && !productoRecomendadoTexto.isBlank() ? productoRecomendadoTexto : tipoActividad.name(),
                null, null, null, null, dosis, null, null,
                dosis != null ? TipoCalculoDosis.FIJA_POR_ANIMAL : TipoCalculoDosis.NO_APLICA,
                null, null, null, null, null, null, null,
                categoriaAnimalId != null ? List.of(categoriaAnimalId) : List.of(),
                UnidadEdadActividad.DIAS, ModalidadActividad.MANUAL, new ModalidadConfig.ManualConfig(), false);
    }

    /** Constructor legado más antiguo, sin permiteEdadDesconocida. */
    public PlanSanitarioItem(UUID id, UUID empresaId, UUID planId, TipoActividadSanitaria tipoActividad,
                             UUID productoId, String productoRecomendadoTexto, UUID categoriaAnimalId,
                             SexoAnimal sexoAplicable, Integer edadMinDias, Integer edadMaxDias, BigDecimal dosis,
                             String unidadDosis, Integer frecuenciaDias, int diasAlerta, String viaAdministracion,
                             boolean obligatorio, boolean activo, long version,
                             OrigenRegulatorioActividad origenRegulatorio, String especieAplicable) {
        this(id, empresaId, planId, tipoActividad, productoId, productoRecomendadoTexto, categoriaAnimalId,
                sexoAplicable, edadMinDias, edadMaxDias, dosis, unidadDosis, frecuenciaDias, diasAlerta,
                viaAdministracion, obligatorio, activo, version, origenRegulatorio, especieAplicable, false);
    }
}
