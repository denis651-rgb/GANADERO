package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.domain.PesajeMasivoResultado;

import java.util.List;

public record PesajeMasivoResponse(
        List<PesajeMasivoItemResponse> items,
        int registrados,
        int conError) {

    public static PesajeMasivoResponse from(List<PesajeMasivoResultado> resultados) {
        return new PesajeMasivoResponse(
                resultados.stream().map(PesajeMasivoItemResponse::from).toList(),
                (int) resultados.stream().filter(PesajeMasivoResultado::ok).count(),
                (int) resultados.stream().filter(r -> !r.ok()).count());
    }
}
