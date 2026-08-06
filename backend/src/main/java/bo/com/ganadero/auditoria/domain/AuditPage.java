package bo.com.ganadero.auditoria.domain;

import java.util.List;

public record AuditPage(List<AuditoriaRegistro> content, int page, int size, long totalElements, int totalPages) {
    public static AuditPage of(List<AuditoriaRegistro> content, int page, int size, long total) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new AuditPage(content, page, size, total, totalPages);
    }
}
