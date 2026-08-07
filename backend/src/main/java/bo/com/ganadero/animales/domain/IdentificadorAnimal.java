package bo.com.ganadero.animales.domain;

import java.time.Instant;
import java.util.UUID;

public record IdentificadorAnimal(
        UUID id,
        UUID empresaId,
        UUID animalId,
        TipoIdentificador tipo,
        String valor,
        boolean principal,
        EstadoIdentificador estado,
        Instant fechaAsignacion,
        Instant fechaRetiro,
        String motivoRetiro,
        UUID asignadoPor,
        UUID retiradoPor,
        String observaciones,
        String payload,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    public boolean activo() {
        return estado == EstadoIdentificador.ACTIVO;
    }

    public boolean retirado() {
        return estado == EstadoIdentificador.RETIRADO;
    }

    public boolean esQr() {
        return tipo == TipoIdentificador.QR;
    }
}
