package bo.com.ganadero.pesajes.application;

import java.time.LocalDate;
import java.util.List;

public record PesajeMasivoCommand(
        LocalDate fecha,
        String dispositivo,
        String observaciones,
        List<PesajeMasivoItem> items) {
}
