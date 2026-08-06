package bo.com.ganadero.sync.api;

import java.util.List;

public record LoteResponse(long procesadas, long fallidas, List<OperacionResultado> resultados) {
}
