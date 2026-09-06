package bo.com.ganadero.sanidad.api;

import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.sanidad.application.CrearPlanItemCommand;
import bo.com.ganadero.sanidad.domain.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CrearPlanItemRequest(
        @Size(max = 60) String codigoInterno, @NotBlank @Size(max = 200) String nombre,
        @Size(max = 2000) String descripcion, @NotNull TipoActividadSanitaria tipoActividad,
        @NotNull ModalidadActividad modalidad, @NotNull ModalidadConfig modalidadConfig,
        @Size(max = 300) String productoRecomendadoTexto, @Size(max = 200) String principioActivo,
        @Size(max = 2000) String instruccionesVeterinario, @Size(max = 2000) String observaciones,
        @Positive BigDecimal dosisCantidad, UnidadDosis dosisUnidad, @Size(max = 100) String dosisUnidadDetalle,
        @NotNull TipoCalculoDosis dosisTipoCalculo, @Positive BigDecimal dosisPesoReferenciaKg,
        @Positive BigDecimal dosisMinima, @Positive BigDecimal dosisMaxima, ViaAdministracion viaAdministracionCodigo,
        @Size(max = 100) String viaAdministracionDetalle, LugarAplicacion lugarAplicacion,
        @Size(max = 100) String lugarAplicacionDetalle, List<UUID> categoriasAplicables, SexoAnimal sexoAplicable,
        @PositiveOrZero Integer edadMinDias, @PositiveOrZero Integer edadMaxDias, UnidadEdadActividad edadUnidad,
        Boolean permiteEdadDesconocida, @PositiveOrZero int diasAlerta, boolean obligatorio,
        @NotNull OrigenRegulatorioActividad origenRegulatorio, @Size(max = 60) String especieAplicable,
        @Size(max = 500) String motivoVersion, Instant fechaVigencia) {

    CrearPlanItemCommand command() {
        return new CrearPlanItemCommand(codigoInterno, nombre, descripcion, tipoActividad, modalidad, modalidadConfig,
                productoRecomendadoTexto, principioActivo, instruccionesVeterinario, observaciones, dosisCantidad,
                dosisUnidad, dosisUnidadDetalle, dosisTipoCalculo, dosisPesoReferenciaKg, dosisMinima, dosisMaxima,
                viaAdministracionCodigo, viaAdministracionDetalle, lugarAplicacion, lugarAplicacionDetalle,
                categoriasAplicables == null ? List.of() : categoriasAplicables, sexoAplicable, edadMinDias,
                edadMaxDias, edadUnidad == null ? UnidadEdadActividad.DIAS : edadUnidad,
                Boolean.TRUE.equals(permiteEdadDesconocida), diasAlerta, obligatorio, origenRegulatorio,
                especieAplicable == null || especieAplicable.isBlank() ? "BOVINO" : especieAplicable,
                motivoVersion, fechaVigencia);
    }
}
