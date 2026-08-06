package bo.com.ganadero.empresas.domain;

import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;

import java.time.Instant;
import java.util.UUID;

public class ConfiguracionEmpresa {
    private final UUID empresaId;
    private UnidadPeso unidadPeso;
    private UnidadSuperficie unidadSuperficie;
    private String moneda;
    private int diasAlertaPreparto;
    private int diasAlertaVacunacion;
    private int diasSinPesaje;
    private boolean permitirStockNegativo;
    private boolean requiereAprobacionVenta;
    private boolean comprimirImagenes;
    private int calidadImagen;
    private final Instant createdAt;
    private final UUID createdBy;
    private final Instant updatedAt;
    private UUID updatedBy;
    private final long version;

    public ConfiguracionEmpresa(UUID empresaId, UnidadPeso unidadPeso, UnidadSuperficie unidadSuperficie,
            String moneda, int diasAlertaPreparto, int diasAlertaVacunacion, int diasSinPesaje,
            boolean permitirStockNegativo, boolean requiereAprobacionVenta, boolean comprimirImagenes,
            int calidadImagen, Instant createdAt, UUID createdBy, Instant updatedAt, UUID updatedBy, long version) {
        this.empresaId = empresaId; this.unidadPeso = unidadPeso; this.unidadSuperficie = unidadSuperficie;
        this.moneda = moneda; this.diasAlertaPreparto = diasAlertaPreparto;
        this.diasAlertaVacunacion = diasAlertaVacunacion; this.diasSinPesaje = diasSinPesaje;
        this.permitirStockNegativo = permitirStockNegativo;
        this.requiereAprobacionVenta = requiereAprobacionVenta; this.comprimirImagenes = comprimirImagenes;
        this.calidadImagen = calidadImagen; this.createdAt = createdAt; this.createdBy = createdBy;
        this.updatedAt = updatedAt; this.updatedBy = updatedBy; this.version = version;
    }

    public void update(UnidadPeso unidadPeso, UnidadSuperficie unidadSuperficie, String moneda,
            Integer diasAlertaPreparto, Integer diasAlertaVacunacion, Integer diasSinPesaje,
            Boolean permitirStockNegativo, Boolean requiereAprobacionVenta, Boolean comprimirImagenes,
            Integer calidadImagen, long expectedVersion, UUID actorId) {
        if (version != expectedVersion) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        if (moneda != null && moneda.length() != 3) invalid("La moneda debe tener tres caracteres.");
        if (diasAlertaPreparto != null && diasAlertaPreparto < 0
                || diasAlertaVacunacion != null && diasAlertaVacunacion < 0
                || diasSinPesaje != null && diasSinPesaje < 0) invalid("Los días de alerta no pueden ser negativos.");
        if (calidadImagen != null && (calidadImagen < 1 || calidadImagen > 100))
            invalid("La calidad de imagen debe estar entre 1 y 100.");
        if (unidadPeso != null) this.unidadPeso = unidadPeso;
        if (unidadSuperficie != null) this.unidadSuperficie = unidadSuperficie;
        if (moneda != null) this.moneda = moneda;
        if (diasAlertaPreparto != null) this.diasAlertaPreparto = diasAlertaPreparto;
        if (diasAlertaVacunacion != null) this.diasAlertaVacunacion = diasAlertaVacunacion;
        if (diasSinPesaje != null) this.diasSinPesaje = diasSinPesaje;
        if (permitirStockNegativo != null) this.permitirStockNegativo = permitirStockNegativo;
        if (requiereAprobacionVenta != null) this.requiereAprobacionVenta = requiereAprobacionVenta;
        if (comprimirImagenes != null) this.comprimirImagenes = comprimirImagenes;
        if (calidadImagen != null) this.calidadImagen = calidadImagen;
        updatedBy = actorId;
    }

    private void invalid(String message) { throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, message); }

    public UUID empresaId() { return empresaId; } public UnidadPeso unidadPeso() { return unidadPeso; }
    public UnidadSuperficie unidadSuperficie() { return unidadSuperficie; } public String moneda() { return moneda; }
    public int diasAlertaPreparto() { return diasAlertaPreparto; }
    public int diasAlertaVacunacion() { return diasAlertaVacunacion; } public int diasSinPesaje() { return diasSinPesaje; }
    public boolean permitirStockNegativo() { return permitirStockNegativo; }
    public boolean requiereAprobacionVenta() { return requiereAprobacionVenta; }
    public boolean comprimirImagenes() { return comprimirImagenes; } public int calidadImagen() { return calidadImagen; }
    public Instant createdAt() { return createdAt; } public UUID createdBy() { return createdBy; }
    public Instant updatedAt() { return updatedAt; } public UUID updatedBy() { return updatedBy; }
    public long version() { return version; }
}
