package bo.com.ganadero.reproduccion.application;

import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.AnimalRepository;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.SexoAnimal;
import bo.com.ganadero.reproduccion.domain.Celo;
import bo.com.ganadero.reproduccion.domain.DiagnosticoGestacion;
import bo.com.ganadero.reproduccion.domain.EstadoRegistroReproduccion;
import bo.com.ganadero.reproduccion.domain.MetodoDiagnostico;
import bo.com.ganadero.reproduccion.domain.ReproduccionAnimal;
import bo.com.ganadero.reproduccion.domain.ReproduccionRepository;
import bo.com.ganadero.reproduccion.domain.ResultadoGestacion;
import bo.com.ganadero.reproduccion.domain.Servicio;
import bo.com.ganadero.reproduccion.domain.TipoCelo;
import bo.com.ganadero.reproduccion.domain.TipoServicio;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReproduccionServiceTest {

    private ReproduccionRepository registros;
    private AnimalRepository animales;
    private ApplicationEventPublisher events;
    private TimelineEventPublisher timeline;
    private ReproduccionService service;

    private UUID company;
    private UUID property;
    private UUID hembraId;
    private UUID machoId;

    @BeforeEach
    void setup() {
        registros = mock(ReproduccionRepository.class);
        animales = mock(AnimalRepository.class);
        events = mock(ApplicationEventPublisher.class);
        timeline = mock(TimelineEventPublisher.class);
        company = UUID.randomUUID();
        property = UUID.randomUUID();
        hembraId = UUID.randomUUID();
        machoId = UUID.randomUUID();
        CurrentUser user = new CurrentUser(UUID.randomUUID(), company, UUID.randomUUID(),
                Set.of(), Set.of("REPRODUCCION_REGISTRAR", "REPRODUCCION_VER"), Set.of(property), false);
        service = new ReproduccionService(registros, animales, new UserContext(() -> user), events, timeline);
    }

    @Test
    void registrarCeloCreaElRegistroEnHembraActiva() {
        Animal hembra = hembra(property);
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra));
        when(registros.createCelo(any(Celo.class), any(UUID.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Celo celo = service.registrarCelo(new RegistrarCeloCommand(null, hembraId, LocalDate.now().minusDays(1),
                TipoCelo.VISUAL, null, null, null, null, null, null));

        assertThat(celo.animalId()).isEqualTo(hembraId);
        assertThat(celo.empresaId()).isEqualTo(company);
        assertThat(celo.propiedadId()).isEqualTo(property);
        assertThat(celo.tipoDeteccion()).isEqualTo(TipoCelo.VISUAL);
        verify(registros).createCelo(any(Celo.class), any(UUID.class));
        verify(timeline).publish(any());
    }

    @Test
    void registrarCeloRechazaMacho() {
        Animal macho = macho(property);
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(macho));

        assertThatThrownBy(() -> service.registrarCelo(new RegistrarCeloCommand(null, hembraId,
                LocalDate.now(), TipoCelo.VISUAL, null, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.REPRODUCCION_SOLO_HEMBRA));
        verify(registros, never()).createCelo(any(), any());
    }

    @Test
    void registrarCeloRechazaAnimalInactivo() {
        Animal hembraInactiva = new Animal(hembraId, company, "V001", "Vaca", SexoAnimal.HEMBRA,
                LocalDate.of(2020, 1, 1), false, null, null, null, null, null,
                property, null, null, EstadoAnimal.VENDIDO, null, null, null, null, null, null, 0);
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembraInactiva));

        assertThatThrownBy(() -> service.registrarCelo(new RegistrarCeloCommand(null, hembraId,
                LocalDate.now(), TipoCelo.VISUAL, null, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_NOT_ACTIVE));
    }

    @Test
    void registrarServicioCalculaNumeroDeIntento() {
        Animal hembra = hembra(property);
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra));
        when(registros.countServicios(hembraId, company)).thenReturn(2);
        when(registros.createServicio(any(Servicio.class), any(UUID.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Servicio servicio = service.registrarServicio(new RegistrarServicioCommand(null, hembraId, null,
                LocalDate.now().minusDays(1), TipoServicio.INSEMINACION_ARTIFICIAL, null, null,
                null, null, null, null, null));

        assertThat(servicio.numeroIntento()).isEqualTo(3);
        verify(registros).createServicio(any(Servicio.class), any(UUID.class));
    }

    @Test
    void registrarServicioRechazaMachoIgualALaHembra() {
        Animal hembra = hembra(property);
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra));

        assertThatThrownBy(() -> service.registrarServicio(new RegistrarServicioCommand(null, hembraId, null,
                LocalDate.now(), TipoServicio.MONTA_NATURAL, hembraId, null,
                null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SERVICIO_MACHO_IGUAL));
    }

    @Test
    void registrarServicioValidaQueElCeloSeaDeLaMismaHembra() {
        Animal hembra = hembra(property);
        UUID celoId = UUID.randomUUID();
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra));
        when(registros.findCeloById(celoId, company))
                .thenReturn(Optional.of(celo(UUID.randomUUID(), property)));

        assertThatThrownBy(() -> service.registrarServicio(new RegistrarServicioCommand(null, hembraId, celoId,
                LocalDate.now(), TipoServicio.INSEMINACION_ARTIFICIAL, null, null,
                null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SERVICIO_CELO_INCOMPATIBLE));
    }

    @Test
    void registrarServicioRechazaMachoInvalido() {
        Animal hembra = hembra(property);
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra));
        when(animales.findById(machoId, company)).thenReturn(Optional.of(hembra(property)));

        assertThatThrownBy(() -> service.registrarServicio(new RegistrarServicioCommand(null, hembraId, null,
                LocalDate.now(), TipoServicio.MONTA_NATURAL, machoId, null,
                null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.SERVICIO_MACHO_INVALIDO));
    }

    @Test
    void registrarDiagnosticoPositivoCalculaFechaProbableDeParto() {
        Animal hembra = hembra(property);
        UUID servicioId = UUID.randomUUID();
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra));
        LocalDate fechaServicio = LocalDate.now().minusDays(30);
        LocalDate fechaDiagnostico = LocalDate.now().minusDays(10);
        Servicio servicio = new Servicio(servicioId, company, hembraId, null,
                fechaServicio, TipoServicio.MONTA_NATURAL, machoId, 1, null,
                property, null, null, null, null, EstadoRegistroReproduccion.ACTIVO,
                "V001", "Vaca", "T001", "Toro", null, null, 0);
        when(registros.findServicioById(servicioId, company)).thenReturn(Optional.of(servicio));
        when(registros.createDiagnostico(any(DiagnosticoGestacion.class), any(UUID.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DiagnosticoGestacion diagnostico = service.registrarDiagnostico(
                new RegistrarDiagnosticoCommand(null, hembraId, servicioId,
                        fechaDiagnostico, ResultadoGestacion.POSITIVO, MetodoDiagnostico.ECOGRAFIA,
                        null, null, null, null, null, null));

        assertThat(diagnostico.resultado()).isEqualTo(ResultadoGestacion.POSITIVO);
        assertThat(diagnostico.fechaProbableParto()).isEqualTo(fechaServicio.plusDays(285));
    }

    @Test
    void registrarDiagnosticoRechazaFechaAnteriorAlServicio() {
        Animal hembra = hembra(property);
        UUID servicioId = UUID.randomUUID();
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra));
        Servicio servicio = new Servicio(servicioId, company, hembraId, null,
                LocalDate.now().minusDays(5), TipoServicio.MONTA_NATURAL, machoId, 1, null,
                property, null, null, null, null, EstadoRegistroReproduccion.ACTIVO,
                "V001", "Vaca", "T001", "Toro", null, null, 0);
        when(registros.findServicioById(servicioId, company)).thenReturn(Optional.of(servicio));

        assertThatThrownBy(() -> service.registrarDiagnostico(
                new RegistrarDiagnosticoCommand(null, hembraId, servicioId,
                        LocalDate.now().minusDays(10), ResultadoGestacion.NEGATIVO, null,
                        null, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.DIAGNOSTICO_FECHA_ANTERIOR_SERVICIO));
    }

    @Test
    void reproduccionAnimalAgrupaCelosServiciosYDiagnosticos() {
        when(animales.findById(hembraId, company)).thenReturn(Optional.of(hembra(property)));
        when(registros.celosDeAnimal(hembraId, company)).thenReturn(List.of(celo(hembraId, property)));
        when(registros.serviciosDeAnimal(hembraId, company)).thenReturn(List.of());
        when(registros.diagnosticosDeAnimal(hembraId, company)).thenReturn(List.of());

        ReproduccionAnimal animal = service.reproduccionAnimal(hembraId);

        assertThat(animal.animalId()).isEqualTo(hembraId);
        assertThat(animal.celos()).hasSize(1);
        assertThat(animal.servicios()).isEmpty();
        assertThat(animal.diagnosticos()).isEmpty();
    }

    @Test
    void usuarioSinPermisoNoAccede() {
        CurrentUser sinPermiso = new CurrentUser(UUID.randomUUID(), company, UUID.randomUUID(),
                Set.of(), Set.of(), Set.of(property), false);
        ReproduccionService restringido = new ReproduccionService(registros, animales,
                new UserContext(() -> sinPermiso), events, timeline);

        assertThatThrownBy(() -> restringido.reproduccionAnimal(hembraId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.USER_NOT_AUTHORIZED));
        verify(animales, never()).findById(any(), any());
    }

    private Animal hembra(UUID property) {
        return new Animal(hembraId, company, "V001", "Vaca", SexoAnimal.HEMBRA,
                LocalDate.of(2020, 1, 1), false, null, null, null, null, null,
                property, null, null, EstadoAnimal.ACTIVO, null, null, null, null, null, null, 0);
    }

    private Animal macho(UUID property) {
        return new Animal(machoId, company, "T001", "Toro", SexoAnimal.MACHO,
                LocalDate.of(2019, 1, 1), false, null, null, null, null, null,
                property, null, null, EstadoAnimal.ACTIVO, null, null, null, null, null, null, 0);
    }

    private Celo celo(UUID animalId, UUID property) {
        return new Celo(UUID.randomUUID(), company, animalId, LocalDate.now().minusDays(3),
                TipoCelo.VISUAL, null, property, null, null, null, null,
                EstadoRegistroReproduccion.ACTIVO, "V001", "Vaca", null, null, 0);
    }
}
