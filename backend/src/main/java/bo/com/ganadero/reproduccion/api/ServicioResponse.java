package bo.com.ganadero.reproduccion.api;

import bo.com.ganadero.reproduccion.domain.EstadoServicio;
import bo.com.ganadero.reproduccion.domain.Servicio;
import bo.com.ganadero.reproduccion.domain.TipoServicio;

import java.time.Instant;
import java.util.UUID;

public record ServicioResponse(
        UUID id,
        UUID hembraId,
        String codigoAnimal,
        String nombreAnimal,
        UUID celoId,
        Instant fechaServicio,
        TipoServicio tipoServicio,
        UUID machoId,
        String codigoMacho,
        String nombreMacho,
        String codigoSemen,
        String proveedorSemen,
        UUID tecnicoId,
        int numeroIntento,
        Instant fechaDiagnosticoRecomendada,
        String observaciones,
        UUID propiedadId,
        String propiedadNombre,
        UUID potreroId,
        String potreroNombre,
        UUID loteId,
        UUID clienteUuid,
        EstadoServicio estado,
        long version) {

    public static ServicioResponse from(Servicio s) {
        return new ServicioResponse(s.id(), s.hembraId(), s.codigoAnimal(), s.nombreAnimal(), s.celoId(),
                s.fechaServicio(), s.tipoServicio(), s.machoId(), s.codigoMacho(), s.nombreMacho(),
                s.codigoSemen(), s.proveedorSemen(), s.tecnicoId(), s.numeroIntento(),
                s.fechaDiagnosticoRecomendada(), s.observaciones(), s.propiedadId(), s.propiedadNombre(),
                s.potreroId(), s.potreroNombre(), s.loteId(), s.clienteUuid(), s.estado(), s.version());
    }
}
