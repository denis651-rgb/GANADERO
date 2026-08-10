package bo.com.ganadero.pesajes.api;

import bo.com.ganadero.pesajes.application.PesajeMasivoCommand;
import bo.com.ganadero.pesajes.application.PesajeMasivoItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record PesajeMasivoRequest(
        LocalDate fecha,
        @Size(max = 200) String dispositivo,
        @Size(max = 1000) String observaciones,
        @NotEmpty @Valid List<PesajeMasivoItemRequest> items) {

    PesajeMasivoCommand command() {
        return new PesajeMasivoCommand(fecha, dispositivo, observaciones,
                items.stream().map(item -> new PesajeMasivoItem(
                        item.id(), item.animalId(), item.fecha(), item.pesoKg(), item.tipo(),
                        item.condicionCorporal(), item.bascula(), item.propiedadId(), item.potreroId(),
                        item.loteId(), item.observaciones())).toList());
    }
}
