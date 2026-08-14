package bo.com.ganadero.reproduccion.domain;

import java.util.List;

public record ServicioPage(List<Servicio> content, int page, int size, long totalElements, int totalPages) {
    public static ServicioPage of(List<Servicio> content, int page, int size, long total) {
        return new ServicioPage(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
