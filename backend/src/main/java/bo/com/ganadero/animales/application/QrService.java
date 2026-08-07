package bo.com.ganadero.animales.application;

import bo.com.ganadero.animales.domain.*;
import bo.com.ganadero.animales.qr.AnimalQrPayload;
import bo.com.ganadero.animales.qr.QrImageGenerator;
import bo.com.ganadero.animales.qr.QrPayloadService;
import bo.com.ganadero.animales.qr.QrProperties;
import bo.com.ganadero.animales.qr.QrRateLimiter;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.security.CurrentUser;
import bo.com.ganadero.shared.security.UserContext;
import bo.com.ganadero.timeline.application.RegistrarEventoTimeline;
import bo.com.ganadero.timeline.application.TimelineEventPublisher;
import bo.com.ganadero.timeline.domain.TipoEventoAnimal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class QrService {
    private static final Set<Integer> ALLOWED_SIZES = Set.of(256, 512, 1024);

    private final QrProperties properties;
    private final QrPayloadService payloads;
    private final QrImageGenerator images;
    private final QrRateLimiter rateLimiter;
    private final IdentificadorRepository identificadores;
    private final AnimalRepository animales;
    private final UserContext context;
    private final ApplicationEventPublisher events;
    private final TimelineEventPublisher timeline;

    public QrService(QrProperties properties, QrPayloadService payloads, QrImageGenerator images,
                     QrRateLimiter rateLimiter, IdentificadorRepository identificadores,
                     AnimalRepository animales, UserContext context, ApplicationEventPublisher events,
                     TimelineEventPublisher timeline) {
        this.properties = properties;
        this.payloads = payloads;
        this.images = images;
        this.rateLimiter = rateLimiter;
        this.identificadores = identificadores;
        this.animales = animales;
        this.context = context;
        this.events = events;
        this.timeline = timeline;
    }

    @Transactional
    public IdentificadorAnimal generate(UUID animalId, boolean principal) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_ASIGNAR");
        Animal animal = requireAnimal(user, animalId);
        if (animal.estado() != EstadoAnimal.ACTIVO) throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);
        IdentificadorAnimal existing = identificadores.findActiveQr(animalId, user.empresaId()).orElse(null);
        if (existing != null) return existing;
        if (principal) {
            identificadores.lockActiveIdentifiers(animalId, user.empresaId());
            identificadores.clearPrincipal(animalId, user.empresaId(), null);
        }
        UUID identifierId = UUID.randomUUID();
        AnimalQrPayload payload = payloads.sign(animalId, identifierId);
        Instant now = Instant.now();
        IdentificadorAnimal value = new IdentificadorAnimal(
                identifierId, user.empresaId(), animalId, TipoIdentificador.QR, identifierId.toString(),
                principal, EstadoIdentificador.ACTIVO, now, null, null,
                user.userId(), null, null, payloads.toJson(payload), now, now, 0);
        IdentificadorAnimal saved = identificadores.create(value, user.userId());
        Map<String, Object> metadata = new HashMap<>(qrMetadata(saved));
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), animalId, TipoEventoAnimal.QR_ASIGNADO, null,
                "Se generó el código QR para el animal.", null, saved.id(), metadata, user.userId(), Instant.now(), null));
        audit(user, "GENERAR_QR", saved.id(), metadata);
        return saved;
    }

    @Transactional(readOnly = true)
    public QrResolveResult resolve(String json) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_VER");
        if (json == null || json.getBytes(StandardCharsets.UTF_8).length > properties.maxPayloadBytes()) {
            throw new BusinessException(ErrorCode.INVALID_QR);
        }
        rateLimiter.check(user.userId());
        AnimalQrPayload payload;
        try {
            payload = payloads.parse(json);
        } catch (BusinessException exception) {
            auditInvalid(user);
            throw exception;
        }
        if (!payloads.verify(payload)) {
            auditInvalid(user);
            throw new BusinessException(ErrorCode.INVALID_QR);
        }
        if (payload.version() != properties.payloadVersion()) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_QR_VERSION);
        }
        IdentificadorAnimal identifier = identificadores.findByQrIdentifier(payload.identifierId(), user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QR_NOT_FOUND));
        if (!identifier.animalId().equals(payload.animalId())) {
            throw new BusinessException(ErrorCode.QR_NOT_FOUND);
        }
        Animal animal = animales.findById(identifier.animalId(), user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QR_NOT_FOUND));
        if (!context.hasPropertyAccess(user, animal.propiedadActualId())) {
            throw new BusinessException(ErrorCode.QR_NOT_FOUND);
        }
        if (identifier.retirado()) {
            return QrResolveResult.retired(animal, identifier);
        }
        return QrResolveResult.valid(animal, identifier);
    }

    @Transactional
    public IdentificadorAnimal replace(UUID animalId, UUID identificadorId, String motivo, Boolean principal, long version) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_ASIGNAR");
        Animal animal = requireAnimal(user, animalId);
        if (animal.estado() != EstadoAnimal.ACTIVO) throw new BusinessException(ErrorCode.ANIMAL_NOT_ACTIVE);
        if (motivo == null || motivo.isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        IdentificadorAnimal current = identificadores.findById(identificadorId, animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        if (!current.esQr()) throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        if (current.retirado()) throw new BusinessException(ErrorCode.IDENTIFIER_ALREADY_RETIRED);
        identificadores.retire(identificadorId, animalId, user.empresaId(), motivo, version, user.userId());
        boolean newPrincipal = principal == null ? current.principal() : principal;
        if (newPrincipal) {
            identificadores.lockActiveIdentifiers(animalId, user.empresaId());
            identificadores.clearPrincipal(animalId, user.empresaId(), null);
        }
        UUID identifierId = UUID.randomUUID();
        AnimalQrPayload payload = payloads.sign(animalId, identifierId);
        Instant now = Instant.now();
        IdentificadorAnimal value = new IdentificadorAnimal(
                identifierId, user.empresaId(), animalId, TipoIdentificador.QR, identifierId.toString(),
                newPrincipal, EstadoIdentificador.ACTIVO, now, null, null,
                user.userId(), null, null, payloads.toJson(payload), now, now, 0);
        IdentificadorAnimal saved = identificadores.create(value, user.userId());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("qrAnterior", current.id().toString());
        metadata.put("qrNuevo", saved.id().toString());
        metadata.put("motivo", motivo);
        timeline.publish(new RegistrarEventoTimeline(user.empresaId(), animalId, TipoEventoAnimal.QR_REEMPLAZADO, null,
                "Se reemplazó el código QR anterior por uno nuevo.", null, saved.id(), metadata,
                user.userId(), Instant.now(), null));
        audit(user, "REEMPLAZAR_QR", saved.id(), metadata);
        return saved;
    }

    @Transactional(readOnly = true)
    public QrImageResult image(UUID animalId, UUID identificadorId, String format, int size) {
        CurrentUser user = context.requirePermission("IDENTIFICADOR_VER");
        Animal animal = requireAnimal(user, animalId);
        IdentificadorAnimal current = identificadores.findById(identificadorId, animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND));
        if (!current.esQr() || current.payload() == null) {
            throw new BusinessException(ErrorCode.IDENTIFIER_NOT_FOUND);
        }
        if (!ALLOWED_SIZES.contains(size)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "El tamaño del QR debe ser 256, 512 o 1024.");
        }
        String normalized = format == null ? "png" : format.toLowerCase();
        String extension = "svg".equals(normalized) ? "svg" : "png";
        String filename = "animal-" + safe(animal.codigo()) + "-qr." + extension;
        if ("svg".equals(normalized)) {
            return new QrImageResult("image/svg+xml; charset=utf-8", filename,
                    images.svg(current.payload(), size, current.retirado()).getBytes(StandardCharsets.UTF_8));
        }
        return new QrImageResult("image/png", filename, images.png(current.payload(), size, current.retirado()));
    }

    private Animal requireAnimal(CurrentUser user, UUID animalId) {
        Animal animal = animales.findById(animalId, user.empresaId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
        context.requirePropertyAccess(user, animal.propiedadActualId());
        return animal;
    }

    private Map<String, Object> qrMetadata(IdentificadorAnimal i) {
        return Map.of("estado", i.estado().name(), "tipo", i.tipo().name(), "valor", i.valor(), "principal", i.principal());
    }

    private void audit(CurrentUser user, String accion, UUID id, Map<String, Object> datos) {
        events.publishEvent(new AnimalAuditEvent(user.empresaId(), user.userId(), accion, "IDENTIFICADOR", id, Instant.now(), datos));
    }

    private void auditInvalid(CurrentUser user) {
        events.publishEvent(new AnimalAuditEvent(user.empresaId(), user.userId(), "RESOLVER_QR", "IDENTIFICADOR", null,
                Instant.now(), Map.of("resultado", "INVALID_QR")));
    }

    private String safe(String value) {
        return value == null ? "sin-codigo" : value.replaceAll("[^a-zA-Z0-9-_]", "-");
    }

    public record QrImageResult(String contentType, String filename, byte[] bytes) {}

    public record QrResolveResult(
            boolean valid,
            String code,
            String message,
            AnimalInfo animal,
            IdentifierInfo identifier) {

        public static QrResolveResult valid(Animal animal, IdentificadorAnimal identifier) {
            return new QrResolveResult(true, "ACTIVE", "Identificación válida.",
                    AnimalInfo.of(animal), IdentifierInfo.of(identifier));
        }

        public static QrResolveResult retired(Animal animal, IdentificadorAnimal identifier) {
            return new QrResolveResult(false, "QR_RETIRED",
                    "El código QR fue retirado y ya no está vigente.",
                    AnimalInfo.of(animal), IdentifierInfo.of(identifier));
        }

        public record AnimalInfo(UUID id, String codigo, String nombre, String sexo, String estado,
                                 UUID propiedadActualId, UUID potreroActualId) {
            static AnimalInfo of(Animal a) {
                return new AnimalInfo(a.id(), a.codigo(), a.nombre(), a.sexo().name(), a.estado().name(),
                        a.propiedadActualId(), a.potreroActualId());
            }
        }

        public record IdentifierInfo(UUID id, String tipo, String valor, String estado, boolean principal,
                                     Instant fechaAsignacion, Instant fechaRetiro) {
            static IdentifierInfo of(IdentificadorAnimal i) {
                return new IdentifierInfo(i.id(), i.tipo().name(), i.valor(), i.estado().name(), i.principal(),
                        i.fechaAsignacion(), i.fechaRetiro());
            }
        }
    }
}
