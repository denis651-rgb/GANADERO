package bo.com.ganadero.sync.application;

import bo.com.ganadero.animales.application.AnimalService;
import bo.com.ganadero.animales.application.IdentificadorService;
import bo.com.ganadero.animales.domain.Animal;
import bo.com.ganadero.animales.domain.EstadoAnimal;
import bo.com.ganadero.animales.domain.IdentificadorAnimal;
import bo.com.ganadero.movimientos.application.MovimientoService;
import bo.com.ganadero.movimientos.domain.Movimiento;
import bo.com.ganadero.pesajes.application.PesajeService;
import bo.com.ganadero.pesajes.domain.Pesaje;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.sync.api.*;
import bo.com.ganadero.sync.domain.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class SyncService {
    private final SyncRepository sync;
    private final AnimalService animales;
    private final IdentificadorService identificadores;
    private final PesajeService pesajes;
    private final MovimientoService movimientos;
    private final UserContext context;
    private final ObjectMapper objectMapper;

    public SyncService(SyncRepository sync, AnimalService animales, IdentificadorService identificadores,
                       PesajeService pesajes, MovimientoService movimientos, UserContext context,
                       ObjectMapper objectMapper) {
        this.sync = sync;
        this.animales = animales;
        this.identificadores = identificadores;
        this.pesajes = pesajes;
        this.movimientos = movimientos;
        this.context = context;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SyncDispositivoResponse registrarDispositivo(SyncPushRequest.DispositivoInfo info) {
        CurrentUser user = context.requirePermission("SINC_DISPOSITIVO_REGISTRAR");
        Dispositivo d = requireDispositivo(user, info);
        return new SyncDispositivoResponse(d.id(), d.codigoDispositivo(), d.nombre(), d.plataforma(), d.versionApp(),
                d.estado().name(), d.ultimoCursor());
    }

    @Transactional(readOnly = true)
    public SyncPullResponse pull(SyncPushRequest.DispositivoInfo info, long cursor, int size) {
        CurrentUser user = context.requirePermission("SINC_PULL");
        Dispositivo dispositivo = requireDispositivo(user, info);
        List<CambioSync> cambios = sync.pullCambios(user.empresaId(), cursor, size);
        long nuevoCursor = cambios.isEmpty() ? cursor : cambios.get(cambios.size() - 1).id();
        boolean hayMas = sync.hasCambiosDespues(user.empresaId(), nuevoCursor);
        sync.upsertDispositivo(withCursor(dispositivo, nuevoCursor));
        List<CambioResponse> responses = cambios.stream()
                .map(c -> new CambioResponse(c.id(), c.tabla(), c.entidadId(), c.tipoCambio(),
                        parse(c.datosJson()), c.dispositivoOrigen(), c.createdAt()))
                .toList();
        return new SyncPullResponse(dispositivo.id(), nuevoCursor, hayMas, Instant.now(), responses);
    }

    @Transactional(readOnly = true)
    public SyncBootstrapResponse bootstrap(SyncPushRequest.DispositivoInfo info) {
        CurrentUser user = context.requirePermission("SINC_BOOTSTRAP");
        Dispositivo dispositivo = requireDispositivo(user, info);
        UUID empresa = user.empresaId();
        boolean todas = user.accesoTodasPropiedades();
        Set<UUID> permitidas = user.propiedadesPermitidas();
        Map<String, Object> empresaData = sync.bootstrapEmpresas(empresa).stream().findFirst().orElse(Map.of());
        long cursor = sync.ultimoCursor(empresa);
        sync.upsertDispositivo(withCursor(dispositivo, cursor));
        return new SyncBootstrapResponse(dispositivo.id(), cursor, empresaData,
                sync.bootstrapPropiedades(empresa, todas, permitidas),
                sync.bootstrapSectores(empresa, todas, permitidas),
                sync.bootstrapPotreros(empresa, todas, permitidas),
                sync.bootstrapLotes(empresa, todas, permitidas),
                sync.bootstrapRazas(empresa),
                sync.bootstrapCategorias(empresa),
                sync.bootstrapTiposPasto(empresa),
                sync.bootstrapAnimales(empresa, todas, permitidas),
                sync.bootstrapIdentificadores(empresa, todas, permitidas),
                sync.bootstrapPesajes(empresa, todas, permitidas),
                sync.bootstrapMembresias(empresa, todas, permitidas),
                new SyncUsuarioInfo(user.userId(), user.roles(), user.permisos(), permitidas, todas));
    }

    @Transactional
    public SyncPushResponse push(SyncPushRequest request) {
        CurrentUser user = context.requirePermission("SINC_PUSH");
        Dispositivo dispositivo = requireDispositivo(user, request.dispositivo());
        sync.setDispositivoOrigen(dispositivo.codigoDispositivo());
        List<OperacionResultado> resultados = new ArrayList<>();
        for (SyncPushRequest.OperacionRequest op : request.operaciones()) {
            resultados.add(applyOperacion(user, dispositivo, op));
        }
        long cursor = sync.ultimoCursor(user.empresaId());
        sync.upsertDispositivo(withCursor(dispositivo, cursor));
        return new SyncPushResponse(dispositivo.id(), cursor, resultados);
    }

    @Transactional
    public LoteResponse lote(List<SyncPushRequest.OperacionRequest> operaciones) {
        List<OperacionResultado> resultados = operaciones.stream().map(this::applyLoteOperacion).toList();
        long procesadas = resultados.stream().filter(r -> "SYNCED".equals(r.estado())).count();
        return new LoteResponse(procesadas, resultados.size() - procesadas, resultados);
    }

    private OperacionResultado applyLoteOperacion(SyncPushRequest.OperacionRequest request) {
        try {
            Object saved = dispatch(context.currentUser(), request);
            long version = serverVersion(saved, request.tipo());
            UUID entityId = serverEntityId(saved, request.tipo());
            return new OperacionResultado(request.clienteId(), "SYNCED", entityId, version, toNode(saved), null, null, null);
        } catch (BusinessException exception) {
            if (exception.code() == ErrorCode.VERSION_CONFLICT) {
                Object serverData = currentServerData(context.currentUser(), request);
                long version = serverVersion(serverData, request.tipo());
                return new OperacionResultado(request.clienteId(), "CONFLICT", request.entidadId(), version,
                        toNode(serverData), exception.code().name(), exception.getMessage(), List.of("version"));
            }
            return new OperacionResultado(request.clienteId(), "REJECTED", request.entidadId(), null, null,
                    exception.code().name(), exception.getMessage(), null);
        } catch (RuntimeException exception) {
            return new OperacionResultado(request.clienteId(), "RETRYABLE", request.entidadId(), null, null,
                    "INTERNAL_ERROR", exception.getMessage(), null);
        }
    }

    private OperacionResultado applyOperacion(CurrentUser user, Dispositivo dispositivo,
                                              SyncPushRequest.OperacionRequest request) {
        Optional<OperacionSync> existing = sync.findOperacion(user.empresaId(), dispositivo.id(), request.clienteId());
        if (existing.isPresent() && !"PENDIENTE".equals(existing.get().estado())) {
            return fromStored(existing.get());
        }
        OperacionSync op = new OperacionSync(UUID.randomUUID(), user.empresaId(), dispositivo.id(), user.userId(),
                request.clienteId(), request.tipo(), request.entidad(), request.entidadId(), writeJson(request.datos()),
                request.versionCliente() == null ? 0 : request.versionCliente(), "PENDIENTE", null, null, null,
                null, null, request.idempotencyKey(), Instant.now(), null);
        try {
            Object saved = dispatch(user, request);
            long serverVersion = serverVersion(saved, request.tipo());
            UUID entityId = serverEntityId(saved, request.tipo());
            sync.saveOperacion(op.conResultado("APLICADA", null, null, writeJson(saved), serverVersion, null, entityId));
            return new OperacionResultado(request.clienteId(), "SYNCED", entityId, serverVersion,
                    toNode(saved), null, null, null);
        } catch (BusinessException exception) {
            if (exception.code() == ErrorCode.VERSION_CONFLICT) {
                Object serverData = currentServerData(user, request);
                long serverVersion = serverVersion(serverData, request.tipo());
                List<String> fields = List.of("version");
                sync.saveOperacion(op.conResultado("CONFLICTO", exception.code().name(), exception.getMessage(),
                        writeJson(serverData), serverVersion, writeJson(fields), request.entidadId()));
                return new OperacionResultado(request.clienteId(), "CONFLICT", request.entidadId(), serverVersion,
                        toNode(serverData), exception.code().name(), exception.getMessage(), fields);
            }
            sync.saveOperacion(op.conResultado("RECHAZADA", exception.code().name(), exception.getMessage(),
                    null, null, null, request.entidadId()));
            return new OperacionResultado(request.clienteId(), "REJECTED", request.entidadId(), null, null,
                    exception.code().name(), exception.getMessage(), null);
        } catch (RuntimeException exception) {
            sync.saveOperacion(op.conResultado("ERROR", "INTERNAL_ERROR", exception.getMessage(),
                    null, null, null, request.entidadId()));
            return new OperacionResultado(request.clienteId(), "RETRYABLE", request.entidadId(), null, null,
                    "INTERNAL_ERROR", exception.getMessage(), null);
        }
    }

    private Object dispatch(CurrentUser user, SyncPushRequest.OperacionRequest request) {
        Map<String, Object> datos = request.datos() == null ? Map.of() : request.datos();
        return switch (request.tipo()) {
            case "ANIMAL_CREAR" -> animales.create(convert(datos, bo.com.ganadero.animales.application.AnimalCommand.class));
            case "ANIMAL_ACTUALIZAR" -> animales.update(uuid(required(datos, "id")),
                    convert(datos, bo.com.ganadero.animales.application.AnimalCommand.class));
            case "ANIMAL_CAMBIAR_ESTADO" -> animales.changeState(uuid(required(datos, "id")),
                    EstadoAnimal.valueOf(str(required(datos, "estado"))), str(datos.get("motivo")),
                    longValue(datos.get("version")));
            case "IDENTIFICADOR_ASIGNAR" -> identificadores.assign(uuid(required(datos, "animalId")),
                    convert(datos, bo.com.ganadero.animales.application.IdentificadorCommand.class));
            case "PESAJE_REGISTRAR" -> pesajes.registrar(convert(datos, bo.com.ganadero.pesajes.application.PesajeCommand.class));
            case "MOVIMIENTO_CREAR" -> movimientos.create(convert(datos, bo.com.ganadero.movimientos.application.MovimientoCommand.class));
            case "MOVIMIENTO_CONFIRMAR" -> movimientos.confirm(uuid(required(datos, "id")), longValue(datos.get("version")));
            case "MOVIMIENTO_ANULAR" -> movimientos.annul(uuid(required(datos, "id")), str(datos.get("motivo")),
                    longValue(datos.get("version")));
            default -> throw new BusinessException(ErrorCode.OPERACION_TIPO_DESCONOCIDO);
        };
    }

    private OperacionResultado fromStored(OperacionSync op) {
        String estado = switch (op.estado()) {
            case "APLICADA" -> "SYNCED";
            case "CONFLICTO" -> "CONFLICT";
            case "RECHAZADA" -> "REJECTED";
            case "ERROR" -> "RETRYABLE";
            default -> "DUPLICATED";
        };
        UUID entityId = op.entidadId() != null ? op.entidadId() : idFromJson(op.resultadoServidorJson());
        return new OperacionResultado(op.clienteId(), estado, entityId, op.versionServidor(),
                parse(op.resultadoServidorJson()), op.resultadoCodigo(), op.resultadoMensaje(),
                parseList(op.conflictosJson()));
    }

    private Object currentServerData(CurrentUser user, SyncPushRequest.OperacionRequest request) {
        UUID id = request.datos() != null ? uuid(request.datos().get("id")) : null;
        if (id == null) return null;
        return switch (request.tipo()) {
            case "ANIMAL_ACTUALIZAR", "ANIMAL_CAMBIAR_ESTADO" -> animales.get(id);
            case "MOVIMIENTO_CONFIRMAR", "MOVIMIENTO_ANULAR" -> movimientos.get(id);
            case "PESAJE_REGISTRAR" -> pesajes.get(id);
            default -> null;
        };
    }

    private long serverVersion(Object saved, String tipo) {
        if (saved == null) return 0;
        return switch (tipo) {
            case "ANIMAL_CREAR", "ANIMAL_ACTUALIZAR", "ANIMAL_CAMBIAR_ESTADO" -> ((Animal) saved).version();
            case "IDENTIFICADOR_ASIGNAR" -> ((IdentificadorAnimal) saved).version();
            case "PESAJE_REGISTRAR" -> ((Pesaje) saved).version();
            case "MOVIMIENTO_CREAR", "MOVIMIENTO_CONFIRMAR", "MOVIMIENTO_ANULAR" -> ((Movimiento) saved).version();
            default -> 0;
        };
    }

    private UUID serverEntityId(Object saved, String tipo) {
        if (saved == null) return null;
        return switch (tipo) {
            case "ANIMAL_CREAR", "ANIMAL_ACTUALIZAR", "ANIMAL_CAMBIAR_ESTADO" -> ((Animal) saved).id();
            case "IDENTIFICADOR_ASIGNAR" -> ((IdentificadorAnimal) saved).id();
            case "PESAJE_REGISTRAR" -> ((Pesaje) saved).id();
            case "MOVIMIENTO_CREAR", "MOVIMIENTO_CONFIRMAR", "MOVIMIENTO_ANULAR" -> ((Movimiento) saved).id();
            default -> null;
        };
    }

    private Dispositivo requireDispositivo(CurrentUser user, SyncPushRequest.DispositivoInfo info) {
        Dispositivo d = new Dispositivo(UUID.randomUUID(), user.empresaId(), user.userId(), info.codigo(),
                info.nombre(), info.plataforma(), info.versionApp(), EstadoDispositivo.ACTIVO, Instant.now(), 0, 0);
        Dispositivo saved = sync.upsertDispositivo(d);
        if (saved.estado() == EstadoDispositivo.BLOQUEADO) throw new BusinessException(ErrorCode.DISPOSITIVO_BLOQUEADO);
        return saved;
    }

    private Dispositivo withCursor(Dispositivo d, long cursor) {
        return new Dispositivo(d.id(), d.empresaId(), d.usuarioId(), d.codigoDispositivo(), d.nombre(), d.plataforma(),
                d.versionApp(), d.estado(), d.ultimoSeenAt(), cursor, d.version());
    }

    private <T> T convert(Map<String, Object> datos, Class<T> type) {
        return objectMapper.convertValue(datos, type);
    }

    private Object required(Map<String, Object> datos, String key) {
        Object value = datos.get(key);
        if (value == null) throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Campo requerido en operación: " + key);
        return value;
    }

    private UUID uuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }

    private long longValue(Object value) {
        if (value == null) return 0;
        if (value instanceof Number number) return number.longValue();
        return Long.parseLong(value.toString());
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private JsonNode toNode(Object value) {
        return value == null ? null : objectMapper.valueToTree(value);
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            return null;
        }
    }

    private List<String> parseList(String json) {
        JsonNode node = parse(json);
        if (node == null || !node.isArray()) return null;
        List<String> values = new ArrayList<>();
        node.forEach(item -> values.add(item.asText()));
        return values;
    }

    private UUID idFromJson(String json) {
        JsonNode node = parse(json);
        if (node == null || !node.has("id")) return null;
        return uuid(node.get("id"));
    }
}
