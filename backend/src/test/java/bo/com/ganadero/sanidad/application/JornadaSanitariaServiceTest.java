package bo.com.ganadero.sanidad.application;

import bo.com.ganadero.alertas.application.MotorAlertas;
import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.sanidad.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JornadaSanitariaServiceTest {
    private JornadaSanitariaRepository jornadas;
    private SanidadRepository planes;
    private AnimalRepository animales;
    private JornadaSanitariaService service;
    private UUID empresa;
    private UUID jornadaId;
    private UUID planId;
    private UUID itemId;
    private UUID propiedadId;
    private UUID potreroId;
    private UUID categoriaNovillo;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        jornadas = mock(JornadaSanitariaRepository.class);
        planes = mock(SanidadRepository.class);
        animales = mock(AnimalRepository.class);
        empresa = UUID.randomUUID();
        jornadaId = UUID.randomUUID();
        planId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        propiedadId = UUID.randomUUID();
        potreroId = UUID.randomUUID();
        categoriaNovillo = UUID.randomUUID();
        CurrentUser user = new CurrentUser(UUID.randomUUID(), empresa, UUID.randomUUID(), Set.of(),
                Set.of("SANIDAD_VER", "SANIDAD_JORNADA_CREAR", "SANIDAD_JORNADA_CONFIRMAR"), Set.of(), true);
        service = new JornadaSanitariaService(jornadas, planes, animales, new UserContext(() -> user),
                mock(ObjectProvider.class), mock(ObjectProvider.class), mock(TimelineEventPublisher.class),
                mock(ApplicationEventPublisher.class));
    }

    @Test
    void separaElegiblesYExplicaTodosLosMotivosDeExclusion() {
        LocalDate aplicacion = LocalDate.of(2026, 8, 14);
        UUID categoriaToro = UUID.randomUUID();
        Animal apto = animal("ANI-001", categoriaNovillo, aplicacion.minusDays(200));
        Animal excluido = animal("ANI-002", categoriaToro, aplicacion.minusDays(900));
        prepararDatos(TipoActividadSanitaria.VITAMINIZACION, List.of(apto, excluido));

        ResultadoElegibilidad resultado = service.elegibilidad(jornadaId, itemId, aplicacion);

        assertThat(resultado.elegibles()).extracting(AnimalElegibilidad::codigo).containsExactly("ANI-001");
        assertThat(resultado.noElegibles()).hasSize(1);
        assertThat(resultado.noElegibles().getFirst().motivos())
                .contains("La categoría del animal no coincide con la actividad.",
                        "Tiene 900 días; la actividad permite como máximo 360 días.");
    }

    @Test
    void rechazaActividadDeTipoDistintoAlDeLaJornada() {
        prepararDatos(TipoActividadSanitaria.VACUNACION, List.of());

        assertThatThrownBy(() -> service.elegibilidad(jornadaId, itemId, LocalDate.of(2026, 8, 14)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.SANIDAD_ITEM_JORNADA_INCOMPATIBLE));
    }

    private void prepararDatos(TipoActividadSanitaria tipoItem, List<Animal> candidatos) {
        JornadaSanitaria jornada = new JornadaSanitaria(jornadaId, empresa, TipoActividadSanitaria.VITAMINIZACION,
                LocalDate.of(2026, 8, 14), null, propiedadId, potreroId, null, UUID.randomUUID(), null,
                EstadoJornada.BORRADOR, null, null, 0);
        PlanSanitario plan = new PlanSanitario(planId, empresa, "Plan", null, LocalDate.of(2026, 1, 1), null,
                EstadoPlanSanitario.ACTIVO, null, null, 0);
        PlanSanitarioItem item = new PlanSanitarioItem(itemId, empresa, planId, tipoItem, null, "Producto",
                categoriaNovillo, SexoAnimal.MACHO, 60, 360, null, null, 60, 5, null, true, true, 0);
        when(jornadas.buscar(jornadaId, empresa)).thenReturn(Optional.of(jornada));
        when(planes.planes(empresa)).thenReturn(List.of(plan));
        when(planes.items(planId, empresa, false)).thenReturn(List.of(item));
        when(animales.findEligible(empresa, propiedadId, null, null, null)).thenReturn(candidatos);
    }

    private Animal animal(String codigo, UUID categoria, LocalDate nacimiento) {
        return new Animal(UUID.randomUUID(), empresa, codigo, codigo, SexoAnimal.MACHO, nacimiento, false,
                UUID.randomUUID(), categoria, null, PropositoAnimal.CARNE, OrigenAnimal.COMPRADO, propiedadId,
                potreroId, null, EstadoAnimal.ACTIVO, LocalDate.of(2026, 1, 1), null, null, null, null, null, 0);
    }
}
