package bo.com.ganadero.animales.qr;

import java.util.UUID;

public record AnimalQrPayload(
        String type,
        UUID animalId,
        UUID identifierId,
        int version,
        String signature) {

    public static final String TYPE = "GANADERO_ANIMAL";

    public boolean hasValidStructure() {
        return TYPE.equals(type)
                && animalId != null
                && identifierId != null
                && version > 0
                && signature != null
                && signature.length() == 64;
    }
}
