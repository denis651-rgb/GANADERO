package bo.com.ganadero.reproduccion.domain;

import java.util.List;

/**
 * Resultado de registrar un parto: el parto persistido junto con sus crías
 * (incluye los animales creados automáticamente para las crías vivas).
 */
public record PartoResult(Parto parto, List<CriaParto> crias) {
}
