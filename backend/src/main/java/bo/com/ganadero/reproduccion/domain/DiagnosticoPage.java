package bo.com.ganadero.reproduccion.domain;

import java.util.List;

public record DiagnosticoPage(List<DiagnosticoGestacion> content, int page, int size, long totalElements, int totalPages) {
    public static DiagnosticoPage of(List<DiagnosticoGestacion> content, int page, int size, long total) {
        return new DiagnosticoPage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
