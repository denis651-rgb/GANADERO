package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.domain.DiagnosticoPage;

import java.util.List;

public record DiagnosticoPageResponse(List<DiagnosticoGestacionResponse> content, int page, int size, long totalElements, int totalPages) {
    public static DiagnosticoPageResponse from(DiagnosticoPage p) {
        return new DiagnosticoPageResponse(p.content().stream().map(DiagnosticoGestacionResponse::from).toList(),
                p.page(), p.size(), p.totalElements(), p.totalPages());
    }
}
