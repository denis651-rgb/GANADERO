package bo.com.ganadero.ventas.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentaRepository {
    Venta create(Venta venta);

    Optional<Venta> findById(UUID id);

    List<Venta> findAll(UUID animalId, LocalDate desde, LocalDate hasta);
}
