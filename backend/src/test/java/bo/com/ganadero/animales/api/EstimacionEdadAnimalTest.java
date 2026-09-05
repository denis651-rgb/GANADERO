package bo.com.ganadero.animales.api;

import bo.com.ganadero.animales.domain.FuenteEdadDeclarada;
import bo.com.ganadero.animales.domain.UnidadEdadDeclarada;
import bo.com.ganadero.shared.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EstimacionEdadAnimalTest {
    @Test
    void conservaLaEdadDeclaradaEstructuradaYCalculaElNacimientoEstimado() {
        var resultado = EstimacionEdadAnimal.resolver(null, false, 18, UnidadEdadDeclarada.MESES,
                LocalDate.of(2026, 9, 4), FuenteEdadDeclarada.PROVEEDOR, "Informada por Estancia El Roble");

        assertThat(resultado.fechaNacimiento()).isEqualTo(LocalDate.of(2025, 3, 4));
        assertThat(resultado.estimada()).isTrue();
        assertThat(resultado.valorDeclarado()).isEqualTo(18);
        assertThat(resultado.unidad()).isEqualTo(UnidadEdadDeclarada.MESES);
        assertThat(resultado.fechaReferencia()).isEqualTo(LocalDate.of(2026, 9, 4));
        assertThat(resultado.fuente()).isEqualTo(FuenteEdadDeclarada.PROVEEDOR);
        assertThat(resultado.detalle()).isEqualTo("Informada por Estancia El Roble");
    }

    @Test
    void calculaEnDias() {
        var resultado = EstimacionEdadAnimal.resolver(null, false, 45, UnidadEdadDeclarada.DIAS,
                LocalDate.of(2026, 9, 4), FuenteEdadDeclarada.ESTIMACION_CAMPO, null);

        assertThat(resultado.fechaNacimiento()).isEqualTo(LocalDate.of(2026, 7, 21));
    }

    @Test
    void calculaEnAnios() {
        var resultado = EstimacionEdadAnimal.resolver(null, false, 3, UnidadEdadDeclarada.ANIOS,
                LocalDate.of(2026, 9, 4), FuenteEdadDeclarada.ESTIMACION_CAMPO, null);

        assertThat(resultado.fechaNacimiento()).isEqualTo(LocalDate.of(2023, 9, 4));
    }

    @Test
    void edadDesconocidaNoInventaFechaNiMarcaEstimada() {
        var resultado = EstimacionEdadAnimal.resolver(null, null, null, null, null, null, null);

        assertThat(resultado.fechaNacimiento()).isNull();
        assertThat(resultado.estimada()).isFalse();
        assertThat(resultado.valorDeclarado()).isNull();
    }

    @Test
    void fechaConocidaNoSeMezclaConEdadAproximada() {
        assertThatThrownBy(() -> EstimacionEdadAnimal.resolver(LocalDate.of(2025, 3, 4), false, 18,
                UnidadEdadDeclarada.MESES, LocalDate.of(2026, 9, 4), FuenteEdadDeclarada.PROVEEDOR, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rechazaReferenciaFutura() {
        LocalDate manana = LocalDate.now(java.time.ZoneId.of("America/La_Paz")).plusDays(1);
        assertThatThrownBy(() -> EstimacionEdadAnimal.resolver(null, false, 6, UnidadEdadDeclarada.MESES,
                manana, FuenteEdadDeclarada.PROVEEDOR, null))
                .isInstanceOf(BusinessException.class);
    }
}
