package bo.com.ganadero.reproduccion.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto de persistencia del módulo reproduccion.
 *
 * <p>Como en el resto del monolito modular, los métodos reciben siempre el
 * {@code empresaId} y opcionalmente las propiedades permitidas del usuario
 * para garantizar el filtrado de acceso por propiedad.</p>
 */
public interface ReproduccionRepository {

    CeloPage findAllCelos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                          UUID animalId, Instant fechaDesde, Instant fechaHasta, IntensidadCelo intensidad,
                          EstadoRegistroReproduccion estado, UUID propiedadId, int page, int size);

    Optional<Celo> findCeloById(UUID id, UUID empresa);

    Optional<Celo> findCeloByClienteUuid(UUID clienteUuid, UUID empresa);

    List<Celo> celosDeAnimal(UUID animalId, UUID empresa);

    Celo createCelo(Celo celo, UUID actor);

    Celo annulCelo(UUID id, UUID empresa, String motivo, long version, UUID actor);

    ServicioPage findAllServicios(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                  UUID animalId, UUID propiedadId, int page, int size);

    Optional<Servicio> findServicioById(UUID id, UUID empresa);

    Optional<Servicio> findServicioByClienteUuid(UUID clienteUuid, UUID empresa);

    List<Servicio> serviciosDeAnimal(UUID animalId, UUID empresa);

    Servicio createServicio(Servicio servicio, UUID actor);

    int countServicios(UUID animalId, UUID empresa);

    void updateServicioEstado(UUID id, UUID empresa, EstadoServicio estado, UUID actor);

    DiagnosticoPage findAllDiagnosticos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                        UUID animalId, UUID propiedadId, int page, int size);

    Optional<DiagnosticoGestacion> findDiagnosticoById(UUID id, UUID empresa);

    Optional<DiagnosticoGestacion> findDiagnosticoByClienteUuid(UUID clienteUuid, UUID empresa);

    List<DiagnosticoGestacion> diagnosticosDeAnimal(UUID animalId, UUID empresa);

    DiagnosticoGestacion createDiagnostico(DiagnosticoGestacion diagnostico, UUID actor);
    void updateDiagnosticoResultado(UUID id, UUID empresa, ResultadoGestacion resultado, UUID actor);

    PartoPage findAllPartos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                            UUID animalId, UUID propiedadId, int page, int size);

    Optional<Parto> findPartoById(UUID id, UUID empresa);

    Optional<Parto> findPartoByClienteUuid(UUID clienteUuid, UUID empresa);

    List<Parto> partosDeAnimal(UUID animalId, UUID empresa);

    Parto createParto(Parto parto, UUID actor);
    boolean existsActivePartoForGestacion(UUID empresa, UUID gestacionId);

    CriaPartoPage findAllCrias(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                               UUID partoId, int page, int size);

    Optional<CriaParto> findCriaById(UUID id, UUID empresa);

    Optional<CriaParto> findCriaByClienteUuid(UUID clienteUuid, UUID empresa);

    List<CriaParto> criasDeParto(UUID partoId, UUID empresa);

    CriaParto createCria(CriaParto cria, UUID actor);
    Optional<CriaParto> findCriaByAnimal(UUID animalId, UUID empresa);

    AbortoPage findAllAbortos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                              UUID animalId, UUID propiedadId, int page, int size);

    Optional<Aborto> findAbortoById(UUID id, UUID empresa);

    Optional<Aborto> findAbortoByClienteUuid(UUID clienteUuid, UUID empresa);

    List<Aborto> abortosDeAnimal(UUID animalId, UUID empresa);

    Aborto createAborto(Aborto aborto, UUID actor);

    DestetePage findAllDestetes(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                UUID animalId, UUID propiedadId, int page, int size);

    Optional<Destete> findDesteteById(UUID id, UUID empresa);

    Optional<Destete> findDesteteByClienteUuid(UUID clienteUuid, UUID empresa);

    List<Destete> destetesDeAnimal(UUID animalId, UUID empresa);

    Destete createDestete(Destete destete, UUID actor);
}
