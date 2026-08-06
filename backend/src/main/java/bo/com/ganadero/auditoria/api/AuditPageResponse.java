package bo.com.ganadero.auditoria.api;

import bo.com.ganadero.auditoria.domain.AuditPage;

import java.util.List;

public record AuditPageResponse(List<AuditoriaResponse> content, int page, int size,
                                long totalElements, int totalPages) {
    public static AuditPageResponse from(AuditPage page) {
        return new AuditPageResponse(
                page.content().stream().map(AuditoriaResponse::from).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages());
    }
}
