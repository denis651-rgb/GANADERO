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
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SyncService {
    private static final int MAX_ATTEMPTS = 6;
    private static final int MAX_MENSAJE_LEN = 500;
    private static final int MAX_ERROR_LEN = 2000;

    private final SyncRepository sync;
    private final AnimalService animales;
    private final IdentificadorService identificadores;
    private final PesajeService pesajes;
    private final MovimientoService movimientos;
    private final UserContext context;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate tx;
    private final TransactionTemplate retryTx;

    public SyncService(SyncRepository sync, AnimalService animales, IdentificadorService identificadores,
                       PesajeService pesajes, MovimientoService movimientos, UserContext context,
                       ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.sync = sync;
        this.animales = animales;
        this.identificadores = identificadores;
        this.pesajes = pesajes;
        this.movimientos = movimientos;
        this.context = context;
        this.objectMapper = objectMapper;
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        this.retryTx = new TransactionTemplate(transactionManager);
        this.retryTx.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public SyncDispositivoResponse registrarDispositivo(SyncPushRequest.DispositivoInfo info) {
        CurrentUser user = context.requirePermission("SINC_DISPOSITIVO_REGISTRAR");
        Dispositivo d = requireDispositivo(user, info);
        return new SyncDispositivoResponse(d.id(), d.codigoDispositivo(), d.nombre(), d.plataforma(), d.versionApp(),
                d.estado().name(), d.ultimoCursor());
    }

    @Transactional
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

    @Transactional
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

    public SyncPushResponse push(SyncPushRequest request) {
        CurrentUser user = context.requirePermission("SINC_PUSH");
        Dispositivo dispositivo = requireDispositivo(user, request.dispositivo());
        List<OperacionResultado> resultados = new ArrayList<>();
        for (SyncPushRequest.OperacionRequest op : request.operaciones()) {
            resultados.add(procesar(user, dispositivo, op));
        }
        long cursor = sync.ultimoCursor(user.empresaId());
        sync.upsertDispositivo(withCursor(dispositivo, cursor));
        return new SyncPushResponse(dispositivo.id(), cursor, resultados);
    }

    public LoteResponse lote(List<SyncPushRequest.OperacionRequest> operaciones) {
        CurrentUser user = context.requirePermission("SINC_PUSH");
        Dispositivo dispositivo = requireDispositivo(user,
                new SyncPushRequest.DispositivoInfo("web-lote", "Operaciones web", "WEB", "1.0.0"));
        List<OperacionResultado> resultados = new ArrayList<>();
        for (SyncPushRequest.OperacionRequest op : operaciones) {
            resultados.add(procesar(user, dispositivo, op));
        }
        long procesadas = resultados.stream().filter(r -> "SYNCED".equals(r.estado())).count();
        return new LoteResponse(procesadas, resultados.size() - procesadas, resultados);
    }

    private OperacionResultado procesar(CurrentUser user, Dispositivo dispositivo,
                                        SyncPushRequest.OperacionRequest request) {
        try {
            return tx.execute(status -> aplicar(user, dispositivo, request));
        } catch (RuntimeException reason) {
            return registrarRetryable(user, dispositivo, request, reason);
        }
    }

    private OperacionResultado aplicar(CurrentUser user, Dispositivo dispositivo,
                                       SyncPushRequest.OperacionRequest request) {
        sync.setDispositivoOrigen(dispositivo.codigoDispositivo(), dispositivo.id());
        String hash = payloadHash(request.datos());

        Optional<OperacionSync> porClave = sync.findOperacionByIdempotencyKey(user.empresaId(), request.idempotencyKey());
        if (porClave.isPresent()) {
            OperacionSync existing = porClave.get();
            if (!existing.dispositivoId().equals(dispositivo.id())) {
                return conflict(user, dispositivo, request, hash, existing,
                        "Clave de idempotencia reutilizada por otro dispositivo.");
            }
            if (existing.estado() == EstadoOperacionSync.SYNCED) {
                if (Objects.equals(existing.payloadHash(), hash)) return fromStored(existing);
                return conflict(user, dispositivo, request, hash, existing,
                        "Clave de idempotencia reutilizada con un payload distinto.");
            }
            if (esDefinitivo(existing.estado())) return fromStored(existing);
        }

        Optional<OperacionSync> porCliente = sync.findOperacion(user.empresaId(), dispositivo.id(), request.clienteId());
        if (porCliente.isPresent()) {
            OperacionSync existing = porCliente.get();
            if (existing.estado() == EstadoOperacionSync.SYNCED) {
                if (Objects.equals(existing.payloadHash(), hash)) return fromStored(existing);
                return conflict(user, dispositivo, request, hash, existing,
                        "El id local fue reutilizado con un payload distinto.");
            }
            if (esDefinitivo(existing.estado())) return fromStored(existing);
        }

        OperacionSync op = baseOperacion(user, dispositivo, request, hash, porCliente.orElse(null));
        sync.saveOperacion(op.conResultado(EstadoOperacionSync.PROCESSING, null, null, null, null, null,
                request.entidadId()));
        try {
            Object saved = dispatch(user, request);
            long serverVersion = serverVersion(saved, request.tipo());
            UUID entityId = serverEntityId(saved, request.tipo());
            sync.saveOperacion(op.conResultado(EstadoOperacionSync.SYNCED, null, null, writeJson(saved),
                    serverVersion, null, entityId));
            return new OperacionResultado(request.clienteId(), EstadoOperacionSync.SYNCED.name(), entityId,
                    serverVersion, toNode(saved), null, null, null);
        } catch (BusinessException exception) {
            if (exception.code() == ErrorCode.VERSION_CONFLICT) {
                Object serverData = currentServerData(user, request);
                long serverVersion = serverVersion(serverData, request.tipo());
                List<String> fields = List.of("version");
                sync.saveOperacion(op.conResultado(EstadoOperacionSync.CONFLICT, exception.code().name(),
                        truncate(exception.getMessage(), MAX_MENSAJE_LEN), writeJson(serverData), serverVersion,
                        writeJson(fields), request.entidadId()));
                return new OperacionResultado(request.clienteId(), EstadoOperacionSync.CONFLICT.name(),
                        request.entidadId(), serverVersion, toNode(serverData), exception.code().name(),
                        truncate(exception.getMessage(), MAX_MENSAJE_LEN), fields);
            }
            sync.saveOperacion(op.conResultado(EstadoOperacionSync.REJECTED, exception.code().name(),
                    truncate(exception.getMessage(), MAX_MENSAJE_LEN), null, null, null, request.entidadId()));
            return new OperacionResultado(request.clienteId(), EstadoOperacionSync.REJECTED.name(),
                    request.entidadId(), null, null, exception.code().name(),
                    truncate(exception.getMessage(), MAX_MENSAJE_LEN), null);
        }
    }

    private OperacionResultado conflict(CurrentUser user, Dispositivo dispositivo,
                                        SyncPushRequest.OperacionRequest request, String hash,
                                        OperacionSync existing, String motivo) {
        OperacionSync op = baseOperacion(user, dispositivo, request, hash, existing);
        String message = truncate(motivo, MAX_MENSAJE_LEN);
        sync.saveOperacion(op.conResultado(EstadoOperacionSync.CONFLICT, ErrorCode.IDEMPOTENCY_CONFLICT.name(),
                message, existing.resultadoServidorJson(), existing.versionServidor(), writeJson(List.of("idempotency")),
                existing.entidadId()));
        return new OperacionResultado(request.clienteId(), EstadoOperacionSync.CONFLICT.name(), existing.entidadId(),
                existing.versionServidor(), parse(existing.resultadoServidorJson()),
                ErrorCode.IDEMPOTENCY_CONFLICT.name(), message, List.of("idempotency"));
    }

    private OperacionResultado registrarRetryable(CurrentUser user, Dispositivo dispositivo,
                                                  SyncPushRequest.OperacionRequest request, RuntimeException reason) {
        return retryTx.execute(status -> {
            sync.setDispositivoOrigen(dispositivo.codigoDispositivo(), dispositivo.id());
            String hash = payloadHash(request.datos());
            Optional<OperacionSync> existing = sync.findOperacion(user.empresaId(), dispositivo.id(), request.clienteId());
            OperacionSync op = baseOperacion(user, dispositivo, request, hash, existing.orElse(null));
            int attempts = op.attempts() + 1;
            Instant next = nextRetryAt(attempts);
            String message = truncate(reason.getMessage(), MAX_MENSAJE_LEN);
            sync.saveOperacion(op.conReintento(attempts, next, truncate(reason.getMessage(), MAX_ERROR_LEN))
                    .conResultado(EstadoOperacionSync.RETRYABLE, "INTERNAL_ERROR", message, null, null, null,
                            request.entidadId()));
            return new OperacionResultado(request.clienteId(), EstadoOperacionSync.RETRYABLE.name(),
                    request.entidadId(), null, null, "INTERNAL_ERROR", message, null);
        });
    }

    private OperacionSync baseOperacion(CurrentUser user, Dispositivo dispositivo,
                                        SyncPushRequest.OperacionRequest request, String hash, OperacionSync existing) {
        return new OperacionSync(
                existing == null || !existing.clienteId().equals(request.clienteId())
                        ? UUID.randomUUID() : existing.id(),
                user.empresaId(), dispositivo.id(), user.userId(),
                request.clienteId(), request.tipo(), request.entidad(), request.entidadId(),
                writeJson(request.datos()), request.versionCliente() == null ? 0 : request.versionCliente(),
                EstadoOperacionSync.PENDING, null, null, null, null, null,
                request.idempotencyKey(), hash,
                existing == null ? 0 : existing.attempts(),
                existing == null ? null : existing.nextRetryAt(),
                existing == null ? null : existing.lastError(),
                existing == null ? Instant.now() : existing.createdAt(),
                existing == null ? null : existing.appliedAt());
    }

    private boolean esDefinitivo(EstadoOperacionSync estado) {
        return estado == EstadoOperacionSync.CONFLICT || estado == EstadoOperacionSync.REJECTED;
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
            case "MOVIMIENTO_REVERTIR" -> movimientos.revert(uuid(required(datos, "id")), str(datos.get("motivo")),
                    longValue(datos.get("version")));
            default -> throw new BusinessException(ErrorCode.OPERACION_TIPO_DESCONOCIDO);
        };
    }

    private OperacionResultado fromStored(OperacionSync op) {
        UUID entityId = op.entidadId() != null ? op.entidadId() : idFromJson(op.resultadoServidorJson());
        return new OperacionResultado(op.clienteId(), op.estado().name(), entityId, op.versionServidor(),
                parse(op.resultadoServidorJson()), op.resultadoCodigo(), op.resultadoMensaje(),
                parseList(op.conflictosJson()));
    }

    private Object currentServerData(CurrentUser user, SyncPushRequest.OperacionRequest request) {
        UUID id = request.datos() != null ? uuid(request.datos().get("id")) : null;
        if (id == null) return null;
        return switch (request.tipo()) {
            case "ANIMAL_ACTUALIZAR", "ANIMAL_CAMBIAR_ESTADO" -> animales.get(id);
            case "MOVIMIENTO_CONFIRMAR", "MOVIMIENTO_ANULAR", "MOVIMIENTO_REVERTIR" -> movimientos.get(id);
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
            case "MOVIMIENTO_CREAR", "MOVIMIENTO_CONFIRMAR", "MOVIMIENTO_ANULAR", "MOVIMIENTO_REVERTIR" -> ((Movimiento) saved).version();
            default -> 0;
        };
    }

    private UUID serverEntityId(Object saved, String tipo) {
        if (saved == null) return null;
        return switch (tipo) {
            case "ANIMAL_CREAR", "ANIMAL_ACTUALIZAR", "ANIMAL_CAMBIAR_ESTADO" -> ((Animal) saved).id();
            case "IDENTIFICADOR_ASIGNAR" -> ((IdentificadorAnimal) saved).id();
            case "PESAJE_REGISTRAR" -> ((Pesaje) saved).id();
            case "MOVIMIENTO_CREAR", "MOVIMIENTO_CONFIRMAR", "MOVIMIENTO_ANULAR", "MOVIMIENTO_REVERTIR" -> ((Movimiento) saved).id();
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
        JsonNode id = node.get("id");
        if (id == null || !id.isTextual()) return null;
        return uuid(id.asText());
    }

    private String payloadHash(Map<String, Object> datos) {
        if (datos == null) return null;
        try {
            String canonical = objectMapper.writeValueAsString(canonical(datos));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo calcular el hash del payload", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private Object canonical(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sorted = new TreeMap<>();
            map.forEach((key, item) -> sorted.put(String.valueOf(key), canonical(item)));
            return sorted;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::canonical).collect(Collectors.toList());
        }
        return value;
    }

    private Instant nextRetryAt(int attempts) {
        if (attempts >= MAX_ATTEMPTS) return null;
        long seconds = Math.min(3600L, (long) Math.pow(2, attempts) * 60L);
        return Instant.now().plusSeconds(seconds);
    }

    private String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
