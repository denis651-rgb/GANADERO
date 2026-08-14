package bo.com.ganadero.empresas.infrastructure;

import bo.com.ganadero.empresas.domain.ConfiguracionEmpresa;
import bo.com.ganadero.empresas.domain.UnidadPeso;
import bo.com.ganadero.empresas.domain.UnidadSuperficie;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "configuraciones_empresa", schema = "core")
class ConfiguracionEmpresaJpaEntity {
    @Id @Column(name = "empresa_id") private UUID empresaId;
    @Enumerated(EnumType.STRING) @Column(name = "unidad_peso", nullable = false) private UnidadPeso unidadPeso;
    @Enumerated(EnumType.STRING) @Column(name = "unidad_superficie", nullable = false) private UnidadSuperficie unidadSuperficie;
    @Column(nullable = false) private String moneda;
    @Column(name = "dias_alerta_preparto", nullable = false) private int diasAlertaPreparto;
    @Column(name = "dias_alerta_vacunacion", nullable = false) private int diasAlertaVacunacion;
    @Column(name = "dias_diagnostico_post_servicio", nullable = false) private int diasDiagnosticoPostServicio;
    @Column(name = "dias_gestacion_estimada", nullable = false) private int diasGestacionEstimada;
    @Column(name = "dias_sin_pesaje", nullable = false) private int diasSinPesaje;
    @Column(name = "permitir_stock_negativo", nullable = false) private boolean permitirStockNegativo;
    @Column(name = "requiere_aprobacion_venta", nullable = false) private boolean requiereAprobacionVenta;
    @Column(name = "comprimir_imagenes", nullable = false) private boolean comprimirImagenes;
    @Column(name = "calidad_imagen", nullable = false) private int calidadImagen;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "created_by", updatable = false) private UUID createdBy;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "updated_by") private UUID updatedBy;
    @Version private long version;

    protected ConfiguracionEmpresaJpaEntity() {}

    ConfiguracionEmpresa toDomain() {
        return new ConfiguracionEmpresa(empresaId, unidadPeso, unidadSuperficie, moneda,
                diasAlertaPreparto, diasAlertaVacunacion, diasDiagnosticoPostServicio,
                diasGestacionEstimada, diasSinPesaje, permitirStockNegativo,
                requiereAprobacionVenta, comprimirImagenes, calidadImagen, createdAt, createdBy,
                updatedAt, updatedBy, version);
    }

    void apply(ConfiguracionEmpresa source) {
        unidadPeso = source.unidadPeso(); unidadSuperficie = source.unidadSuperficie(); moneda = source.moneda();
        diasAlertaPreparto = source.diasAlertaPreparto(); diasAlertaVacunacion = source.diasAlertaVacunacion();
        diasDiagnosticoPostServicio = source.diasDiagnosticoPostServicio();
        diasGestacionEstimada = source.diasGestacionEstimada();
        diasSinPesaje = source.diasSinPesaje(); permitirStockNegativo = source.permitirStockNegativo();
        requiereAprobacionVenta = source.requiereAprobacionVenta(); comprimirImagenes = source.comprimirImagenes();
        calidadImagen = source.calidadImagen(); updatedBy = source.updatedBy(); updatedAt = Instant.now();
    }
}
