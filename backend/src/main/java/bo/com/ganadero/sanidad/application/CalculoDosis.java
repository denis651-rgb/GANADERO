package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.pesajes.domain.TipoPeso;

import java.math.BigDecimal;
import java.time.Instant;

/** Resultado del cálculo de dosis (sección 6): qué peso se usó (si aplica) y la dosis resultante. */
public record CalculoDosis(BigDecimal dosisCalculada, BigDecimal pesoUsadoKg, TipoPeso pesoTipo, Instant pesoFecha) {
}
