package bo.com.ganadero.animales.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoriaAnimalTest {
    @Test
    void usaMesesCumplidosParaClasificar() {
        CategoriaAnimal vaquilla = new CategoriaAnimal(UUID.randomUUID(), null, "VAQUILLA", "Vaquilla",
                "HEMBRA", 13, 35, null, true);

        assertThat(vaquilla.appliesTo(SexoAnimal.HEMBRA,
                LocalDate.of(2025, 3, 5), LocalDate.of(2026, 9, 4))).isTrue();
        assertThat(vaquilla.appliesTo(SexoAnimal.MACHO,
                LocalDate.of(2025, 3, 5), LocalDate.of(2026, 9, 4))).isFalse();
    }

    @Test
    void bueyNoSeDeduceSolamentePorEdad() {
        CategoriaAnimal buey = new CategoriaAnimal(UUID.randomUUID(), null, "BUEY", "Buey",
                "MACHO", 24, null, null, true);
        assertThat(buey.clasificacionAutomatica()).isFalse();
    }

    @Test
    void cambiaDeCategoriaExactamenteAlCumplirElLimiteDelRango() {
        CategoriaAnimal ternero = new CategoriaAnimal(UUID.randomUUID(), null, "TERNERO", "Ternero", "MACHO", 0, 11, null, true, true, 0);
        CategoriaAnimal novillo = new CategoriaAnimal(UUID.randomUUID(), null, "NOVILLO", "Novillo", "MACHO", 12, 35, null, true, true, 0);
        LocalDate nacimiento = LocalDate.of(2025, 3, 5);

        assertThat(ternero.appliesTo(SexoAnimal.MACHO, nacimiento, LocalDate.of(2026, 2, 5))).isTrue();
        assertThat(novillo.appliesTo(SexoAnimal.MACHO, nacimiento, LocalDate.of(2026, 2, 5))).isFalse();
        assertThat(ternero.appliesTo(SexoAnimal.MACHO, nacimiento, LocalDate.of(2026, 3, 5))).isFalse();
        assertThat(novillo.appliesTo(SexoAnimal.MACHO, nacimiento, LocalDate.of(2026, 3, 5))).isTrue();
    }

    @Test
    void rangoSinEdadMaximaCubreCualquierEdadPosterior() {
        CategoriaAnimal vaca = new CategoriaAnimal(UUID.randomUUID(), null, "VACA", "Vaca", "HEMBRA", 36, null, null, true, true, 0);
        assertThat(vaca.appliesTo(SexoAnimal.HEMBRA, LocalDate.of(2000, 1, 1), LocalDate.of(2026, 9, 5))).isTrue();
        assertThat(vaca.appliesTo(SexoAnimal.HEMBRA, LocalDate.of(2025, 1, 1), LocalDate.of(2026, 9, 5))).isFalse();
    }

    @Test
    void categoriaInactivaOManualNoParticipaDelCalculoAutomatico() {
        CategoriaAnimal buey = new CategoriaAnimal(UUID.randomUUID(), null, "BUEY", "Buey", "MACHO", 24, null, null, true, false, 0);
        assertThat(buey.clasificacionAutomatica()).isFalse();
        assertThat(buey.appliesTo(SexoAnimal.MACHO, LocalDate.of(2020, 1, 1), LocalDate.of(2026, 9, 5))).isTrue();
    }
}
