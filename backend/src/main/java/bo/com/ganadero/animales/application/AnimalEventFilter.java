package bo.com.ganadero.animales.application;

import java.time.Instant;

public record AnimalEventFilter(String tipo, Instant desde, Instant hasta, int page, int size) {
}
