package bo.com.ganadero.reproduccion.infrastructure;

import bo.com.ganadero.reproduccion.domain.*;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class JdbcReproduccionRepository implements ReproduccionRepository {
    private final JdbcClient jdbc;

    public JdbcReproduccionRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_CELO =
            "select c.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "pt.nombre as potrero_nombre, pr.nombre as propiedad_nombre " +
            "from reproduccion.celos c " +
            "left join ganado.animales a on a.id=c.animal_id " +
            "left join campo.potreros pt on pt.id=c.potrero_id " +
            "left join core.propiedades pr on pr.id=c.propiedad_id ";

    private static final String SELECT_SERVICIO =
            "select s.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "m.codigo as macho_codigo, m.nombre as macho_nombre, " +
            "pt.nombre as potrero_nombre, pr.nombre as propiedad_nombre " +
            "from reproduccion.servicios s " +
            "left join ganado.animales a on a.id=s.hembra_id " +
            "left join ganado.animales m on m.id=s.macho_id " +
            "left join campo.potreros pt on pt.id=s.potrero_id " +
            "left join core.propiedades pr on pr.id=s.propiedad_id ";

    private static final String SELECT_DIAGNOSTICO =
            "select d.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "pt.nombre as potrero_nombre, pr.nombre as propiedad_nombre " +
            "from reproduccion.diagnosticos_gestacion d " +
            "left join ganado.animales a on a.id=d.animal_id " +
            "left join campo.potreros pt on pt.id=d.potrero_id " +
            "left join core.propiedades pr on pr.id=d.propiedad_id ";

    private static final String SELECT_PARTO =
            "select p.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "m.id as macho_id, m.codigo as macho_codigo, m.nombre as macho_nombre, " +
            "pt.nombre as potrero_nombre, pr.nombre as propiedad_nombre " +
            "from reproduccion.partos p " +
            "left join ganado.animales a on a.id=p.madre_id " +
            "left join reproduccion.servicios s on s.id=p.servicio_id " +
            "left join ganado.animales m on m.id=s.macho_id " +
            "left join campo.potreros pt on pt.id=p.potrero_id " +
            "left join core.propiedades pr on pr.id=p.propiedad_id ";

    private static final String SELECT_CRIA =
            "select c.*, a.codigo as animal_codigo, a.nombre as animal_nombre " +
            "from reproduccion.crias_parto c " +
            "join reproduccion.partos p on p.id=c.parto_id " +
            "left join ganado.animales a on a.id=c.animal_cria_id ";

    private static final String SELECT_ABORTO =
            "select ab.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "pt.nombre as potrero_nombre, pr.nombre as propiedad_nombre " +
            "from reproduccion.abortos ab " +
            "left join ganado.animales a on a.id=ab.animal_id " +
            "left join campo.potreros pt on pt.id=ab.potrero_id " +
            "left join core.propiedades pr on pr.id=ab.propiedad_id ";

    private static final String SELECT_DESTETE =
            "select d.*, a.codigo as animal_codigo, a.nombre as animal_nombre, " +
            "pt.nombre as potrero_nombre, pr.nombre as propiedad_nombre " +
            "from reproduccion.destetes d " +
            "left join ganado.animales a on a.id=d.animal_cria_id " +
            "left join campo.potreros pt on pt.id=d.potrero_id " +
            "left join core.propiedades pr on pr.id=d.propiedad_id ";

    @Override
    public CeloPage findAllCelos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                 UUID animalId, Instant fechaDesde, Instant fechaHasta, IntensidadCelo intensidad,
                                 EstadoRegistroReproduccion estado, UUID propiedadId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder filter = new StringBuilder(" where c.empresa_id=:e");
        params.put("e", empresa);
        aplicarFiltroPropiedad(filter, params, "p.propiedad_id", propiedades, todasPropiedades);
        if (animalId != null) {
            filter.append(" and c.animal_id=:animal");
            params.put("animal", animalId);
        }
        if (propiedadId != null) {
            filter.append(" and c.propiedad_id=:property");
            params.put("property", propiedadId);
        }
        if (fechaDesde != null) { filter.append(" and c.fecha_deteccion>=:desde"); params.put("desde", Timestamp.from(fechaDesde)); }
        if (fechaHasta != null) { filter.append(" and c.fecha_deteccion<=:hasta"); params.put("hasta", Timestamp.from(fechaHasta)); }
        if (intensidad != null) { filter.append(" and c.intensidad=:intensidad"); params.put("intensidad", intensidad.name()); }
        if (estado != null) { filter.append(" and c.estado=:estado"); params.put("estado", estado.name()); }
        long total = jdbc.sql("select count(*)" + SELECT_CELO.substring(SELECT_CELO.indexOf("from"))
                + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<Celo> content = jdbc.sql(SELECT_CELO + filter
                + " order by c.fecha_deteccion desc, c.created_at desc limit :limit offset :offset")
                .params(params).query(this::mapCelo).list();
        return CeloPage.of(content, page, size, total);
    }

    @Override
    public Optional<Celo> findCeloById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_CELO + " where c.id=:id and c.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::mapCelo).optional();
    }

    @Override
    public Optional<Celo> findCeloByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_CELO + " where c.cliente_uuid=:cliente and c.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::mapCelo).optional();
    }

    @Override
    public List<Celo> celosDeAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_CELO + " where c.animal_id=:animal and c.empresa_id=:e"
                + " order by c.fecha_deteccion asc, c.created_at asc")
                .param("animal", animalId).param("e", empresa).query(this::mapCelo).list();
    }

    @Override
    public Celo createCelo(Celo celo, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into reproduccion.celos(id,empresa_id,animal_id,fecha_deteccion,tipo_deteccion,intensidad,
                    detectado_por,observaciones,propiedad_id,potrero_id,lote_id,cliente_uuid,idempotency_key,estado,created_by,updated_by)
                    values(:id,:e,:animal,:fecha,:tipo,:intensidad,:detectado,:observaciones,:propiedad,:potrero,:lote,
                    :cliente,:idempotency,:estado,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(paramsCelo(celo, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (celo.clienteUuid() != null) {
                return findCeloByClienteUuid(celo.clienteUuid(), celo.empresaId()).orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (celo.clienteUuid() != null) {
                return findCeloByClienteUuid(celo.clienteUuid(), celo.empresaId())
                        .orElseGet(() -> findCeloById(celo.id(), celo.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND)));
            }
            return findCeloById(celo.id(), celo.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        }
        return findCeloById(celo.id(), celo.empresaId()).orElseThrow();
    }

    @Override
    public Celo annulCelo(UUID id, UUID empresa, String motivo, long version, UUID actor) {
        int updated = jdbc.sql("update reproduccion.celos set estado='ANULADO', anulado_at=now(), " +
                        "anulado_by=:actor, motivo_anulacion=:motivo, updated_at=now(), updated_by=:actor, " +
                        "version=version+1 where id=:id and empresa_id=:empresa and estado='ACTIVO' and version=:version")
                .param("actor", actor).param("motivo", motivo).param("id", id).param("empresa", empresa)
                .param("version", version).update();
        if (updated == 0) throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        return findCeloById(id, empresa).orElseThrow();
    }

    @Override
    public ServicioPage findAllServicios(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                         UUID animalId, UUID propiedadId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder filter = new StringBuilder(" where s.empresa_id=:e");
        params.put("e", empresa);
        aplicarFiltroPropiedad(filter, params, "s.propiedad_id", propiedades, todasPropiedades);
        if (animalId != null) {
            filter.append(" and s.hembra_id=:animal");
            params.put("animal", animalId);
        }
        if (propiedadId != null) {
            filter.append(" and s.propiedad_id=:property");
            params.put("property", propiedadId);
        }
        long total = jdbc.sql("select count(*)" + SELECT_SERVICIO.substring(SELECT_SERVICIO.indexOf("from"))
                + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<Servicio> content = jdbc.sql(SELECT_SERVICIO + filter
                + " order by s.fecha_servicio desc, s.created_at desc limit :limit offset :offset")
                .params(params).query(this::mapServicio).list();
        return ServicioPage.of(content, page, size, total);
    }

    @Override
    public Optional<Servicio> findServicioById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_SERVICIO + " where s.id=:id and s.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::mapServicio).optional();
    }

    @Override
    public Optional<Servicio> findServicioByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_SERVICIO + " where s.cliente_uuid=:cliente and s.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::mapServicio).optional();
    }

    @Override
    public List<Servicio> serviciosDeAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_SERVICIO + " where s.hembra_id=:animal and s.empresa_id=:e"
                + " order by s.fecha_servicio asc, s.created_at asc")
                .param("animal", animalId).param("e", empresa).query(this::mapServicio).list();
    }

    @Override
    public Servicio createServicio(Servicio servicio, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into reproduccion.servicios(id,empresa_id,hembra_id,celo_id,fecha_servicio,tipo_servicio,
                    macho_id,codigo_semen,proveedor_semen,tecnico_id,numero_intento,fecha_diagnostico_recomendada,
                    observaciones,propiedad_id,potrero_id,lote_id,
                    cliente_uuid,idempotency_key,estado,created_by,updated_by)
                    values(:id,:e,:animal,:celo,:fecha,:tipo,:macho,:codigoSemen,:proveedorSemen,:tecnico,:intento,
                    :fechaDiagnostico,:observaciones,:propiedad,:potrero,:lote,
                    :cliente,:idempotency,:estado,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(paramsServicio(servicio, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (servicio.clienteUuid() != null) {
                return findServicioByClienteUuid(servicio.clienteUuid(), servicio.empresaId()).orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (servicio.clienteUuid() != null) {
                return findServicioByClienteUuid(servicio.clienteUuid(), servicio.empresaId())
                        .orElseGet(() -> findServicioById(servicio.id(), servicio.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND)));
            }
            return findServicioById(servicio.id(), servicio.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        }
        return findServicioById(servicio.id(), servicio.empresaId()).orElseThrow();
    }

    @Override
    public int countServicios(UUID animalId, UUID empresa) {
        Long count = jdbc.sql("select count(*) from reproduccion.servicios " +
                        "where empresa_id=:e and hembra_id=:animal and estado <> 'ANULADO'")
                .param("e", empresa).param("animal", animalId).query(Long.class).single();
        return count == null ? 0 : count.intValue();
    }

    @Override
    public void updateServicioEstado(UUID id, UUID empresa, EstadoServicio estado, UUID actor) {
        jdbc.sql("update reproduccion.servicios set estado=:estado, updated_at=now(), updated_by=:actor, " +
                        "version=version+1 where id=:id and empresa_id=:empresa and estado <> 'ANULADO'")
                .param("estado", estado.name()).param("actor", actor).param("id", id).param("empresa", empresa)
                .update();
    }

    @Override
    public DiagnosticoPage findAllDiagnosticos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                               UUID animalId, UUID propiedadId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder filter = new StringBuilder(" where d.empresa_id=:e");
        params.put("e", empresa);
        aplicarFiltroPropiedad(filter, params, "d.propiedad_id", propiedades, todasPropiedades);
        if (animalId != null) {
            filter.append(" and d.animal_id=:animal");
            params.put("animal", animalId);
        }
        if (propiedadId != null) {
            filter.append(" and d.propiedad_id=:property");
            params.put("property", propiedadId);
        }
        long total = jdbc.sql("select count(*)" + SELECT_DIAGNOSTICO.substring(SELECT_DIAGNOSTICO.indexOf("from"))
                + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<DiagnosticoGestacion> content = jdbc.sql(SELECT_DIAGNOSTICO + filter
                + " order by d.fecha_diagnostico desc, d.created_at desc limit :limit offset :offset")
                .params(params).query(this::mapDiagnostico).list();
        return DiagnosticoPage.of(content, page, size, total);
    }

    @Override
    public Optional<DiagnosticoGestacion> findDiagnosticoById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_DIAGNOSTICO + " where d.id=:id and d.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::mapDiagnostico).optional();
    }

    @Override
    public Optional<DiagnosticoGestacion> findDiagnosticoByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_DIAGNOSTICO + " where d.cliente_uuid=:cliente and d.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::mapDiagnostico).optional();
    }

    @Override
    public List<DiagnosticoGestacion> diagnosticosDeAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_DIAGNOSTICO + " where d.animal_id=:animal and d.empresa_id=:e"
                + " order by d.fecha_diagnostico asc, d.created_at asc")
                .param("animal", animalId).param("e", empresa).query(this::mapDiagnostico).list();
    }

    @Override
    public DiagnosticoGestacion createDiagnostico(DiagnosticoGestacion diagnostico, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into reproduccion.diagnosticos_gestacion(id,empresa_id,animal_id,servicio_id,
                    fecha_diagnostico,resultado,metodo,dias_gestacion_estimados,fecha_probable_parto,veterinario_id,observaciones,
                    propiedad_id,potrero_id,lote_id,cliente_uuid,idempotency_key,estado,created_by,updated_by)
                    values(:id,:e,:animal,:servicio,:fecha,:resultado,:metodo,:diasGestacion,:fechaParto,:veterinario,:observaciones,
                    :propiedad,:potrero,:lote,:cliente,:idempotency,:estado,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(paramsDiagnostico(diagnostico, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (diagnostico.clienteUuid() != null) {
                return findDiagnosticoByClienteUuid(diagnostico.clienteUuid(), diagnostico.empresaId())
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (diagnostico.clienteUuid() != null) {
                return findDiagnosticoByClienteUuid(diagnostico.clienteUuid(), diagnostico.empresaId())
                        .orElseGet(() -> findDiagnosticoById(diagnostico.id(), diagnostico.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND)));
            }
            return findDiagnosticoById(diagnostico.id(), diagnostico.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        }
        return findDiagnosticoById(diagnostico.id(), diagnostico.empresaId()).orElseThrow();
    }

    @Override
    public void updateDiagnosticoResultado(UUID id, UUID empresa, ResultadoGestacion resultado, UUID actor) {
        jdbc.sql("update reproduccion.diagnosticos_gestacion set resultado=:resultado, updated_at=now(), " +
                        "updated_by=:actor, version=version+1 where id=:id and empresa_id=:empresa and estado='ACTIVO'")
                .param("resultado", resultado.name()).param("actor", actor).param("id", id)
                .param("empresa", empresa).update();
    }

    @Override
    public PartoPage findAllPartos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                   UUID animalId, UUID propiedadId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder filter = new StringBuilder(" where p.empresa_id=:e");
        params.put("e", empresa);
        aplicarFiltroPropiedad(filter, params, "p.propiedad_id", propiedades, todasPropiedades);
        if (animalId != null) {
            filter.append(" and p.madre_id=:animal");
            params.put("animal", animalId);
        }
        if (propiedadId != null) {
            filter.append(" and p.propiedad_id=:property");
            params.put("property", propiedadId);
        }
        long total = jdbc.sql("select count(*)" + SELECT_PARTO.substring(SELECT_PARTO.indexOf("from"))
                + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<Parto> content = jdbc.sql(SELECT_PARTO + filter
                + " order by p.fecha_parto desc, p.created_at desc limit :limit offset :offset")
                .params(params).query(this::mapParto).list();
        return PartoPage.of(content, page, size, total);
    }

    @Override
    public Optional<Parto> findPartoById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_PARTO + " where p.id=:id and p.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::mapParto).optional();
    }

    @Override
    public Optional<Parto> findPartoByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_PARTO + " where p.cliente_uuid=:cliente and p.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::mapParto).optional();
    }

    @Override
    public List<Parto> partosDeAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_PARTO + " where p.madre_id=:animal and p.empresa_id=:e"
                + " order by p.fecha_parto asc, p.created_at asc")
                .param("animal", animalId).param("e", empresa).query(this::mapParto).list();
    }

    @Override
    public Parto createParto(Parto parto, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into reproduccion.partos(id,empresa_id,madre_id,servicio_id,diagnostico_gestacion_id,
                    fecha_parto,tipo_parto,dificultad,asistido,responsable_id,resultado_madre,numero_crias,observaciones,
                    propiedad_id,potrero_id,lote_id,cliente_uuid,idempotency_key,estado,created_by,updated_by)
                    values(:id,:e,:animal,:servicio,:diagnostico,:fecha,:tipo,:dificultad,:asistido,:responsable,
                    :resultadoMadre,:numeroCrias,:observaciones,:propiedad,:potrero,:lote,:cliente,:idempotency,:estado,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(paramsParto(parto, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (parto.clienteUuid() != null) {
                return findPartoByClienteUuid(parto.clienteUuid(), parto.empresaId()).orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (parto.clienteUuid() != null) {
                return findPartoByClienteUuid(parto.clienteUuid(), parto.empresaId())
                        .orElseGet(() -> findPartoById(parto.id(), parto.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND)));
            }
            return findPartoById(parto.id(), parto.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        }
        return findPartoById(parto.id(), parto.empresaId()).orElseThrow();
    }

    @Override
    public boolean existsActivePartoForGestacion(UUID empresa, UUID gestacionId) {
        return Boolean.TRUE.equals(jdbc.sql("select exists(select 1 from reproduccion.partos where empresa_id=:e " +
                        "and diagnostico_gestacion_id=:g and estado='ACTIVO')")
                .param("e", empresa).param("g", gestacionId).query(Boolean.class).single());
    }

    @Override
    public CriaPartoPage findAllCrias(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                      UUID partoId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder filter = new StringBuilder(" where c.empresa_id=:e");
        params.put("e", empresa);
        aplicarFiltroPropiedad(filter, params, "c.propiedad_id", propiedades, todasPropiedades);
        if (partoId != null) {
            filter.append(" and c.parto_id=:parto");
            params.put("parto", partoId);
        }
        long total = jdbc.sql("select count(*)" + SELECT_CRIA.substring(SELECT_CRIA.indexOf("from"))
                + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<CriaParto> content = jdbc.sql(SELECT_CRIA + filter
                + " order by c.created_at asc limit :limit offset :offset")
                .params(params).query(this::mapCria).list();
        return CriaPartoPage.of(content, page, size, total);
    }

    @Override
    public Optional<CriaParto> findCriaById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_CRIA + " where c.id=:id and c.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::mapCria).optional();
    }

    @Override
    public Optional<CriaParto> findCriaByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_CRIA + " where c.cliente_uuid=:cliente and c.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::mapCria).optional();
    }

    @Override
    public List<CriaParto> criasDeParto(UUID partoId, UUID empresa) {
        return jdbc.sql(SELECT_CRIA + " where c.parto_id=:parto and c.empresa_id=:e"
                + " order by c.created_at asc")
                .param("parto", partoId).param("e", empresa).query(this::mapCria).list();
    }

    @Override
    public CriaParto createCria(CriaParto cria, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into reproduccion.crias_parto(id,empresa_id,parto_id,animal_cria_id,sexo,
                    peso_nacimiento_kg,estado_nacimiento,hora_nacimiento,observaciones,cliente_uuid,idempotency_key,
                    created_by,updated_by)
                    values(:id,:e,:parto,:animal,:sexo,:peso,:estadoNacimiento,:horaNacimiento,:observaciones,
                    :cliente,:idempotency,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(paramsCria(cria, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (cria.clienteUuid() != null) {
                return findCriaByClienteUuid(cria.clienteUuid(), cria.empresaId()).orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (cria.clienteUuid() != null) {
                return findCriaByClienteUuid(cria.clienteUuid(), cria.empresaId())
                        .orElseGet(() -> findCriaById(cria.id(), cria.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND)));
            }
            return findCriaById(cria.id(), cria.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        }
        return findCriaById(cria.id(), cria.empresaId()).orElseThrow();
    }

    @Override
    public Optional<CriaParto> findCriaByAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_CRIA + " where c.animal_cria_id=:animal and c.empresa_id=:e")
                .param("animal", animalId).param("e", empresa).query(this::mapCria).optional();
    }

    @Override
    public AbortoPage findAllAbortos(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                     UUID animalId, UUID propiedadId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder filter = new StringBuilder(" where ab.empresa_id=:e");
        params.put("e", empresa);
        aplicarFiltroPropiedad(filter, params, "ab.propiedad_id", propiedades, todasPropiedades);
        if (animalId != null) {
            filter.append(" and ab.animal_id=:animal");
            params.put("animal", animalId);
        }
        if (propiedadId != null) {
            filter.append(" and ab.propiedad_id=:property");
            params.put("property", propiedadId);
        }
        long total = jdbc.sql("select count(*)" + SELECT_ABORTO.substring(SELECT_ABORTO.indexOf("from"))
                + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<Aborto> content = jdbc.sql(SELECT_ABORTO + filter
                + " order by ab.fecha_evento desc, ab.created_at desc limit :limit offset :offset")
                .params(params).query(this::mapAborto).list();
        return AbortoPage.of(content, page, size, total);
    }

    @Override
    public Optional<Aborto> findAbortoById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_ABORTO + " where ab.id=:id and ab.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::mapAborto).optional();
    }

    @Override
    public Optional<Aborto> findAbortoByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_ABORTO + " where ab.cliente_uuid=:cliente and ab.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::mapAborto).optional();
    }

    @Override
    public List<Aborto> abortosDeAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_ABORTO + " where ab.animal_id=:animal and ab.empresa_id=:e"
                + " order by ab.fecha_evento asc, ab.created_at asc")
                .param("animal", animalId).param("e", empresa).query(this::mapAborto).list();
    }

    @Override
    public Aborto createAborto(Aborto aborto, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into reproduccion.abortos(id,empresa_id,animal_id,servicio_id,gestacion_id,
                    fecha_evento,edad_gestacional_estimada,causa,diagnostico,veterinario_id,observaciones,propiedad_id,potrero_id,lote_id,
                    cliente_uuid,idempotency_key,estado,created_by,updated_by)
                    values(:id,:e,:animal,:servicio,:diagnostico,:fecha,:edadGestacional,:causa,:diagnosticoTexto,:veterinario,:observaciones,
                    :propiedad,:potrero,:lote,:cliente,:idempotency,:estado,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(paramsAborto(aborto, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (aborto.clienteUuid() != null) {
                return findAbortoByClienteUuid(aborto.clienteUuid(), aborto.empresaId()).orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (aborto.clienteUuid() != null) {
                return findAbortoByClienteUuid(aborto.clienteUuid(), aborto.empresaId())
                        .orElseGet(() -> findAbortoById(aborto.id(), aborto.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND)));
            }
            return findAbortoById(aborto.id(), aborto.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        }
        return findAbortoById(aborto.id(), aborto.empresaId()).orElseThrow();
    }

    @Override
    public DestetePage findAllDestetes(UUID empresa, Set<UUID> propiedades, boolean todasPropiedades,
                                       UUID animalId, UUID propiedadId, int page, int size) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder filter = new StringBuilder(" where d.empresa_id=:e");
        params.put("e", empresa);
        aplicarFiltroPropiedad(filter, params, "d.propiedad_id", propiedades, todasPropiedades);
        if (animalId != null) {
            filter.append(" and d.animal_cria_id=:animal");
            params.put("animal", animalId);
        }
        if (propiedadId != null) {
            filter.append(" and d.propiedad_id=:property");
            params.put("property", propiedadId);
        }
        long total = jdbc.sql("select count(*)" + SELECT_DESTETE.substring(SELECT_DESTETE.indexOf("from"))
                + filter).params(params).query(Long.class).single();
        params.put("limit", size);
        params.put("offset", page * size);
        List<Destete> content = jdbc.sql(SELECT_DESTETE + filter
                + " order by d.fecha_destete desc, d.created_at desc limit :limit offset :offset")
                .params(params).query(this::mapDestete).list();
        return DestetePage.of(content, page, size, total);
    }

    @Override
    public Optional<Destete> findDesteteById(UUID id, UUID empresa) {
        return jdbc.sql(SELECT_DESTETE + " where d.id=:id and d.empresa_id=:e")
                .param("id", id).param("e", empresa).query(this::mapDestete).optional();
    }

    @Override
    public Optional<Destete> findDesteteByClienteUuid(UUID clienteUuid, UUID empresa) {
        return jdbc.sql(SELECT_DESTETE + " where d.cliente_uuid=:cliente and d.empresa_id=:e")
                .param("cliente", clienteUuid).param("e", empresa).query(this::mapDestete).optional();
    }

    @Override
    public List<Destete> destetesDeAnimal(UUID animalId, UUID empresa) {
        return jdbc.sql(SELECT_DESTETE + " where d.animal_cria_id=:animal and d.empresa_id=:e"
                + " order by d.fecha_destete asc, d.created_at asc")
                .param("animal", animalId).param("e", empresa).query(this::mapDestete).list();
    }

    @Override
    public Destete createDestete(Destete destete, UUID actor) {
        int inserted;
        try {
            inserted = jdbc.sql("""
                    insert into reproduccion.destetes(id,empresa_id,animal_cria_id,madre_id,
                    fecha_destete,peso_destete_kg,tipo_destete,motivo,responsable_id,observaciones,propiedad_id,potrero_id,lote_id,
                    cliente_uuid,idempotency_key,estado,created_by,updated_by)
                    values(:id,:e,:animal,:madre,:fecha,:peso,:tipoDestete,:motivo,:responsable,:observaciones,
                    :propiedad,:potrero,:lote,:cliente,:idempotency,:estado,:actor,:actor)
                    on conflict (id) do nothing
                    """).params(paramsDestete(destete, actor)).update();
        } catch (DataIntegrityViolationException ex) {
            if (destete.clienteUuid() != null) {
                return findDesteteByClienteUuid(destete.clienteUuid(), destete.empresaId()).orElseThrow(() -> ex);
            }
            throw ex;
        }
        if (inserted == 0) {
            if (destete.clienteUuid() != null) {
                return findDesteteByClienteUuid(destete.clienteUuid(), destete.empresaId())
                        .orElseGet(() -> findDesteteById(destete.id(), destete.empresaId())
                                .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND)));
            }
            return findDesteteById(destete.id(), destete.empresaId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPRODUCCION_NOT_FOUND));
        }
        return findDesteteById(destete.id(), destete.empresaId()).orElseThrow();
    }

    private void aplicarFiltroPropiedad(StringBuilder filter, Map<String, Object> params, String columna,
                                        Set<UUID> propiedades, boolean todasPropiedades) {
        if (todasPropiedades) return;
        if (propiedades.isEmpty()) {
            filter.append(" and 1=0");
        } else {
            filter.append(" and ").append(columna).append(" in (:allowedProperties)");
            params.put("allowedProperties", propiedades);
        }
    }

    private Map<String, Object> paramsParto(Parto p, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", p.id());
        map.put("e", p.empresaId());
        map.put("animal", p.madreId());
        map.put("servicio", p.servicioId());
        map.put("diagnostico", p.diagnosticoGestacionId());
        map.put("fecha", p.fechaParto());
        map.put("tipo", p.tipoParto().name());
        map.put("dificultad", p.dificultad().name());
        map.put("asistido", p.asistido());
        map.put("responsable", p.responsableId());
        map.put("resultadoMadre", p.resultadoMadre());
        map.put("numeroCrias", p.numeroCrias());
        map.put("observaciones", p.observaciones());
        map.put("propiedad", p.propiedadId());
        map.put("potrero", p.potreroId());
        map.put("lote", p.loteId());
        map.put("cliente", p.clienteUuid());
        map.put("idempotency", p.idempotencyKey());
        map.put("estado", p.estado().name());
        map.put("actor", actor);
        return map;
    }

    private Map<String, Object> paramsCria(CriaParto c, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.id());
        map.put("e", c.empresaId());
        map.put("parto", c.partoId());
        map.put("animal", c.animalCriaId());
        map.put("sexo", c.sexo().name());
        map.put("peso", c.pesoNacimientoKg());
        map.put("estadoNacimiento", c.estadoNacimiento().name());
        map.put("horaNacimiento", c.horaNacimiento());
        map.put("observaciones", c.observaciones());
        map.put("cliente", c.clienteUuid());
        map.put("idempotency", c.idempotencyKey());
        map.put("actor", actor);
        return map;
    }

    private Map<String, Object> paramsAborto(Aborto a, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", a.id());
        map.put("e", a.empresaId());
        map.put("animal", a.animalId());
        map.put("servicio", a.servicioId());
        map.put("diagnostico", a.gestacionId());
        map.put("fecha", a.fechaEvento());
        map.put("edadGestacional", a.edadGestacionalEstimada());
        map.put("causa", a.causa());
        map.put("diagnosticoTexto", a.diagnostico());
        map.put("veterinario", a.veterinarioId());
        map.put("observaciones", a.observaciones());
        map.put("propiedad", a.propiedadId());
        map.put("potrero", a.potreroId());
        map.put("lote", a.loteId());
        map.put("cliente", a.clienteUuid());
        map.put("idempotency", a.idempotencyKey());
        map.put("estado", a.estado().name());
        map.put("actor", actor);
        return map;
    }

    private Map<String, Object> paramsDestete(Destete d, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.id());
        map.put("e", d.empresaId());
        map.put("animal", d.animalCriaId());
        map.put("madre", d.madreId());
        map.put("fecha", d.fechaDestete());
        map.put("peso", d.pesoDesteteKg());
        map.put("tipoDestete", d.tipoDestete().name());
        map.put("motivo", d.motivo());
        map.put("responsable", d.responsableId());
        map.put("observaciones", d.observaciones());
        map.put("propiedad", d.propiedadId());
        map.put("potrero", d.potreroId());
        map.put("lote", d.loteId());
        map.put("cliente", d.clienteUuid());
        map.put("idempotency", d.idempotencyKey());
        map.put("estado", d.estado().name());
        map.put("actor", actor);
        return map;
    }

    private Map<String, Object> paramsCelo(Celo c, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", c.id());
        map.put("e", c.empresaId());
        map.put("animal", c.animalId());
        map.put("fecha", c.fechaDeteccion());
        map.put("tipo", c.tipoDeteccion().name());
        map.put("intensidad", c.intensidad() == null ? null : c.intensidad().name());
        map.put("detectado", c.detectadoPor());
        map.put("observaciones", c.observaciones());
        map.put("propiedad", c.propiedadId());
        map.put("potrero", c.potreroId());
        map.put("lote", c.loteId());
        map.put("cliente", c.clienteUuid());
        map.put("idempotency", c.idempotencyKey());
        map.put("estado", c.estado().name());
        map.put("actor", actor);
        return map;
    }

    private Map<String, Object> paramsServicio(Servicio s, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", s.id());
        map.put("e", s.empresaId());
        map.put("animal", s.hembraId());
        map.put("celo", s.celoId());
        map.put("fecha", s.fechaServicio());
        map.put("tipo", s.tipoServicio().name());
        map.put("macho", s.machoId());
        map.put("codigoSemen", s.codigoSemen());
        map.put("proveedorSemen", s.proveedorSemen());
        map.put("tecnico", s.tecnicoId());
        map.put("intento", s.numeroIntento());
        map.put("fechaDiagnostico", s.fechaDiagnosticoRecomendada());
        map.put("observaciones", s.observaciones());
        map.put("propiedad", s.propiedadId());
        map.put("potrero", s.potreroId());
        map.put("lote", s.loteId());
        map.put("cliente", s.clienteUuid());
        map.put("idempotency", s.idempotencyKey());
        map.put("estado", s.estado().name());
        map.put("actor", actor);
        return map;
    }

    private Map<String, Object> paramsDiagnostico(DiagnosticoGestacion d, UUID actor) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", d.id());
        map.put("e", d.empresaId());
        map.put("animal", d.animalId());
        map.put("servicio", d.servicioId());
        map.put("fecha", d.fechaDiagnostico());
        map.put("resultado", d.resultado().name());
        map.put("metodo", d.metodo() == null ? null : d.metodo().name());
        map.put("diasGestacion", d.diasGestacionEstimados());
        map.put("fechaParto", d.fechaProbableParto());
        map.put("veterinario", d.veterinarioId());
        map.put("observaciones", d.observaciones());
        map.put("propiedad", d.propiedadId());
        map.put("potrero", d.potreroId());
        map.put("lote", d.loteId());
        map.put("cliente", d.clienteUuid());
        map.put("idempotency", d.idempotencyKey());
        map.put("estado", d.estado().name());
        map.put("actor", actor);
        return map;
    }

    private Celo mapCelo(ResultSet r, int row) throws SQLException {
        String tipo = r.getString("tipo_deteccion");
        String intensidad = r.getString("intensidad");
        String estado = r.getString("estado");
        return new Celo(
                r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("animal_id", UUID.class), instant(r, "fecha_deteccion"),
                tipo == null ? null : TipoCelo.valueOf(tipo),
                intensidad == null ? null : IntensidadCelo.valueOf(intensidad),
                r.getObject("detectado_por", UUID.class), r.getString("observaciones"),
                r.getObject("propiedad_id", UUID.class), r.getObject("potrero_id", UUID.class),
                r.getObject("lote_id", UUID.class), r.getObject("cliente_uuid", UUID.class),
                r.getString("idempotency_key"),
                estado == null ? null : EstadoRegistroReproduccion.valueOf(estado),
                instant(r, "anulado_at"), r.getObject("anulado_by", UUID.class),
                r.getString("motivo_anulacion"),
                r.getString("animal_codigo"), r.getString("animal_nombre"),
                r.getString("potrero_nombre"), r.getString("propiedad_nombre"),
                r.getLong("version"));
    }

    private Servicio mapServicio(ResultSet r, int row) throws SQLException {
        String tipo = r.getString("tipo_servicio");
        String estado = r.getString("estado");
        return new Servicio(
                r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("hembra_id", UUID.class), r.getObject("celo_id", UUID.class),
                instant(r, "fecha_servicio"),
                tipo == null ? null : TipoServicio.valueOf(tipo), r.getObject("macho_id", UUID.class),
                r.getString("codigo_semen"), r.getString("proveedor_semen"),
                r.getObject("tecnico_id", UUID.class), r.getInt("numero_intento"),
                instant(r, "fecha_diagnostico_recomendada"), r.getString("observaciones"),
                r.getObject("propiedad_id", UUID.class), r.getObject("potrero_id", UUID.class),
                r.getObject("lote_id", UUID.class), r.getObject("cliente_uuid", UUID.class),
                r.getString("idempotency_key"),
                estado == null ? null : EstadoServicio.valueOf(estado),
                instant(r, "anulado_at"), r.getObject("anulado_by", UUID.class),
                r.getString("motivo_anulacion"),
                r.getString("animal_codigo"), r.getString("animal_nombre"),
                r.getString("macho_codigo"), r.getString("macho_nombre"),
                r.getString("potrero_nombre"), r.getString("propiedad_nombre"),
                r.getLong("version"));
    }

    private DiagnosticoGestacion mapDiagnostico(ResultSet r, int row) throws SQLException {
        String resultado = r.getString("resultado");
        String metodo = r.getString("metodo");
        String estado = r.getString("estado");
        return new DiagnosticoGestacion(
                r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("animal_id", UUID.class), r.getObject("servicio_id", UUID.class),
                instant(r, "fecha_diagnostico"),
                resultado == null ? null : ResultadoGestacion.valueOf(resultado),
                metodo == null ? null : MetodoDiagnostico.valueOf(metodo),
                r.getObject("dias_gestacion_estimados", Integer.class),
                r.getObject("fecha_probable_parto", LocalDate.class),
                r.getObject("veterinario_id", UUID.class), r.getString("observaciones"),
                r.getObject("propiedad_id", UUID.class), r.getObject("potrero_id", UUID.class),
                r.getObject("lote_id", UUID.class), r.getObject("cliente_uuid", UUID.class),
                r.getString("idempotency_key"),
                estado == null ? null : EstadoRegistroReproduccion.valueOf(estado),
                r.getString("animal_codigo"), r.getString("animal_nombre"),
                r.getString("potrero_nombre"), r.getString("propiedad_nombre"),
                r.getLong("version"));
    }

    private Parto mapParto(ResultSet r, int row) throws SQLException {
        return new Parto(r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("madre_id", UUID.class), r.getObject("diagnostico_gestacion_id", UUID.class),
                r.getObject("servicio_id", UUID.class), r.getObject("fecha_parto", LocalDate.class),
                TipoParto.valueOf(r.getString("tipo_parto")), DificultadParto.valueOf(r.getString("dificultad")),
                r.getBoolean("asistido"), r.getObject("responsable_id", UUID.class), r.getString("resultado_madre"),
                r.getInt("numero_crias"), r.getString("observaciones"), r.getObject("propiedad_id", UUID.class),
                r.getObject("potrero_id", UUID.class), r.getObject("lote_id", UUID.class),
                r.getObject("cliente_uuid", UUID.class), r.getString("idempotency_key"),
                EstadoRegistroReproduccion.valueOf(r.getString("estado")), instant(r, "anulado_at"),
                r.getObject("anulado_by", UUID.class), r.getString("motivo_anulacion"), r.getString("animal_codigo"),
                r.getString("animal_nombre"), r.getObject("macho_id", UUID.class), r.getString("macho_codigo"),
                r.getString("macho_nombre"), r.getString("potrero_nombre"), r.getString("propiedad_nombre"),
                r.getLong("version"));
    }

    private CriaParto mapCria(ResultSet r, int row) throws SQLException {
        return new CriaParto(r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("parto_id", UUID.class), r.getObject("animal_cria_id", UUID.class),
                bo.com.ganadero.animales.domain.SexoAnimal.valueOf(r.getString("sexo")),
                r.getBigDecimal("peso_nacimiento_kg"), EstadoNacimiento.valueOf(r.getString("estado_nacimiento")),
                r.getObject("hora_nacimiento", java.time.LocalTime.class), r.getString("observaciones"),
                r.getObject("cliente_uuid", UUID.class),
                r.getString("idempotency_key"), r.getString("animal_codigo"), r.getString("animal_nombre"),
                r.getLong("version"));
    }

    private Aborto mapAborto(ResultSet r, int row) throws SQLException {
        return new Aborto(r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("animal_id", UUID.class), r.getObject("gestacion_id", UUID.class),
                r.getObject("servicio_id", UUID.class), r.getObject("fecha_evento", LocalDate.class),
                r.getObject("edad_gestacional_estimada", Integer.class), r.getString("causa"),
                r.getString("diagnostico"), r.getObject("veterinario_id", UUID.class), r.getString("observaciones"),
                r.getObject("propiedad_id", UUID.class),
                r.getObject("potrero_id", UUID.class), r.getObject("lote_id", UUID.class),
                r.getObject("cliente_uuid", UUID.class), r.getString("idempotency_key"),
                EstadoRegistroReproduccion.valueOf(r.getString("estado")), r.getString("animal_codigo"),
                r.getString("animal_nombre"), r.getString("potrero_nombre"), r.getString("propiedad_nombre"),
                r.getLong("version"));
    }

    private Destete mapDestete(ResultSet r, int row) throws SQLException {
        return new Destete(r.getObject("id", UUID.class), r.getObject("empresa_id", UUID.class),
                r.getObject("animal_cria_id", UUID.class), r.getObject("madre_id", UUID.class),
                r.getObject("fecha_destete", LocalDate.class), r.getBigDecimal("peso_destete_kg"),
                TipoDestete.valueOf(r.getString("tipo_destete")), r.getString("motivo"),
                r.getObject("responsable_id", UUID.class), r.getString("observaciones"),
                r.getObject("propiedad_id", UUID.class),
                r.getObject("potrero_id", UUID.class), r.getObject("lote_id", UUID.class),
                r.getObject("cliente_uuid", UUID.class), r.getString("idempotency_key"),
                EstadoRegistroReproduccion.valueOf(r.getString("estado")), r.getString("animal_codigo"),
                r.getString("animal_nombre"), r.getString("potrero_nombre"), r.getString("propiedad_nombre"),
                r.getLong("version"));
    }

    private static Instant instant(ResultSet r, String column) throws SQLException {
        OffsetDateTime value = r.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
