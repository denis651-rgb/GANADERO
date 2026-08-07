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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QrServiceTest {
    private IdentificadorRepository identificadores;
    private AnimalRepository animales;
    private QrService service;
    private QrPayloadService payloads;
    private UUID company;
    private UUID property;
    private UUID animalId;
    private UUID userId;
    private List<AnimalAuditEvent> events;
    private List<RegistrarEventoTimeline> timelineEvents;

    @BeforeEach
    void setup() {
        company = UUID.randomUUID();
        property = UUID.randomUUID();
        animalId = UUID.randomUUID();
        userId = UUID.randomUUID();
        identificadores = mock(IdentificadorRepository.class);
        animales = mock(AnimalRepository.class);
        QrProperties properties = new QrProperties("test-qr-secret", 1, 2048, 20);
        payloads = new QrPayloadService(properties, new ObjectMapper());
        QrRateLimiter rateLimiter = new QrRateLimiter(properties);
        events = new ArrayList<>();
        timelineEvents = new ArrayList<>();
        CurrentUser user = new CurrentUser(userId, company, UUID.randomUUID(), Set.of(),
                Set.of("IDENTIFICADOR_VER", "IDENTIFICADOR_ASIGNAR"), Set.of(property), true);
        service = new QrService(properties, payloads, new QrImageGenerator(), rateLimiter,
                identificadores, animales, new UserContext(() -> user), event -> events.add((AnimalAuditEvent) event),
                timelineEvents::add);
    }

    @Test
    void generateCreatesSignedQrAndClearsPreviousPrincipal() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findActiveQr(animalId, company)).thenReturn(Optional.empty());
        when(identificadores.create(any(), eq(userId))).thenAnswer(invocation -> invocation.getArgument(0));
        IdentificadorAnimal saved = service.generate(animalId, true);
        assertThat(saved.tipo()).isEqualTo(TipoIdentificador.QR);
        assertThat(saved.principal()).isTrue();
        assertThat(saved.valor()).isEqualTo(saved.id().toString());
        assertThat(saved.payload()).isNotBlank();
        AnimalQrPayload payload = payloads.parse(saved.payload());
        assertThat(payloads.verify(payload)).isTrue();
        assertThat(payload.animalId()).isEqualTo(animalId);
        assertThat(payload.identifierId()).isEqualTo(saved.id());
        verify(identificadores).lockActiveIdentifiers(animalId, company);
        verify(identificadores).clearPrincipal(animalId, company, null);
        assertThat(events).anyMatch(event -> event.accion().equals("GENERAR_QR"));
        assertThat(timelineEvents).anyMatch(event ->
                event.tipo().equals(TipoEventoAnimal.QR_ASIGNADO) && event.animalId().equals(animalId));
    }

    @Test
    void generateReturnsExistingActiveQrInsteadOfDuplicating() {
        IdentificadorAnimal existing = qr(EstadoIdentificador.ACTIVO, false, 3);
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findActiveQr(animalId, company)).thenReturn(Optional.of(existing));
        IdentificadorAnimal result = service.generate(animalId, true);
        assertThat(result.id()).isEqualTo(existing.id());
        verify(identificadores, never()).create(any(), any());
        verify(identificadores, never()).clearPrincipal(any(), any(), any());
    }

    @Test
    void generateRejectsInactiveAnimal() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.VENDIDO)));
        assertThatThrownBy(() -> service.generate(animalId, true))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.ANIMAL_NOT_ACTIVE));
        verify(identificadores, never()).create(any(), any());
    }

    @Test
    void resolveReturnsValidAnimalAndIdentifier() {
        IdentificadorAnimal qr = qr(EstadoIdentificador.ACTIVO, true, 0);
        when(identificadores.findByQrIdentifier(qr.id(), company)).thenReturn(Optional.of(qr));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        QrService.QrResolveResult result = service.resolve(payloads.toJson(payloads.sign(animalId, qr.id())));
        assertThat(result.valid()).isTrue();
        assertThat(result.code()).isEqualTo("ACTIVE");
        assertThat(result.identifier().id()).isEqualTo(qr.id());
        assertThat(result.animal().id()).isEqualTo(animalId);
        assertThat(result.animal().codigo()).isEqualTo("A-1");
    }

    @Test
    void resolveRejectsTamperedPayload() {
        AnimalQrPayload payload = payloads.sign(animalId, UUID.randomUUID());
        AnimalQrPayload tampered = new AnimalQrPayload(payload.type(), animalId, payload.identifierId(),
                payload.version(), "f".repeat(64));
        assertThatThrownBy(() -> service.resolve(payloads.toJson(tampered)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_QR));
    }

    @Test
    void resolveReturnsRetiredQrWithCode() {
        IdentificadorAnimal qr = qr(EstadoIdentificador.RETIRADO, false, 4);
        when(identificadores.findByQrIdentifier(qr.id(), company)).thenReturn(Optional.of(qr));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        QrService.QrResolveResult result = service.resolve(payloads.toJson(payloads.sign(animalId, qr.id())));
        assertThat(result.valid()).isFalse();
        assertThat(result.code()).isEqualTo("QR_RETIRED");
    }

    @Test
    void resolveHidesAnimalsFromOtherCompanies() {
        when(identificadores.findByQrIdentifier(any(), eq(company))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolve(payloads.toJson(payloads.sign(animalId, UUID.randomUUID()))))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.QR_NOT_FOUND));
    }

    @Test
    void resolveRejectsPayloadOverSizeLimit() {
        assertThatThrownBy(() -> service.resolve("x".repeat(2049)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_QR));
    }

    @Test
    void resolveRejectsMalformedJson() {
        assertThatThrownBy(() -> service.resolve("no-es-json"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_QR));
        assertThat(events).anyMatch(event -> event.accion().equals("RESOLVER_QR"));
    }

    @Test
    void resolveEnforcesRateLimit() {
        IdentificadorAnimal qr = qr(EstadoIdentificador.ACTIVO, true, 0);
        when(identificadores.findByQrIdentifier(qr.id(), company)).thenReturn(Optional.of(qr));
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        String json = payloads.toJson(payloads.sign(animalId, qr.id()));
        QrProperties limited = new QrProperties("test-qr-secret", 1, 2048, 2);
        QrRateLimiter strict = new QrRateLimiter(limited);
        QrService strictService = new QrService(limited, payloads, new QrImageGenerator(), strict,
                identificadores, animales, new UserContext(() -> new CurrentUser(userId, company, UUID.randomUUID(),
                Set.of(), Set.of("IDENTIFICADOR_VER", "IDENTIFICADOR_ASIGNAR"), Set.of(property), true)),
                event -> events.add((AnimalAuditEvent) event), timelineEvents::add);
        assertThat(strictService.resolve(json).valid()).isTrue();
        assertThat(strictService.resolve(json).valid()).isTrue();
        assertThatThrownBy(() -> strictService.resolve(json))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.TOO_MANY_REQUESTS));
    }

    @Test
    void replaceRetiresOldQrAndCreatesNewSignedOne() {
        IdentificadorAnimal old = qr(EstadoIdentificador.ACTIVO, true, 2);
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(old.id(), animalId, company)).thenReturn(Optional.of(old));
        when(identificadores.create(any(), eq(userId))).thenAnswer(invocation -> invocation.getArgument(0));
        IdentificadorAnimal nuevo = service.replace(animalId, old.id(), "Desgaste del caraván", null, 2);
        assertThat(nuevo.id()).isNotEqualTo(old.id());
        assertThat(nuevo.principal()).isTrue();
        assertThat(nuevo.payload()).isNotBlank();
        verify(identificadores).retire(old.id(), animalId, company, "Desgaste del caraván", 2, userId);
        assertThat(timelineEvents).anyMatch(event ->
                event.tipo().equals(TipoEventoAnimal.QR_REEMPLAZADO) && event.animalId().equals(animalId));
        assertThat(events).anyMatch(event -> event.accion().equals("REEMPLAZAR_QR"));
    }

    @Test
    void replaceRejectsNonQrIdentifier() {
        IdentificadorAnimal old = identificador(TipoIdentificador.ARETE, "AR-1", false, EstadoIdentificador.ACTIVO, 0);
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(old.id(), animalId, company)).thenReturn(Optional.of(old));
        assertThatThrownBy(() -> service.replace(animalId, old.id(), "motivo", null, 0))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.IDENTIFIER_NOT_FOUND));
        verify(identificadores, never()).retire(any(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void replaceRejectsBlankMotivo() {
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        assertThatThrownBy(() -> service.replace(animalId, UUID.randomUUID(), "   ", null, 0))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void imageRejectsUnsupportedSize() {
        IdentificadorAnimal qr = qr(EstadoIdentificador.ACTIVO, true, 0);
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(qr.id(), animalId, company)).thenReturn(Optional.of(qr));
        assertThatThrownBy(() -> service.image(animalId, qr.id(), "png", 300))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void imageGeneratesPngAndSvgBytes() {
        IdentificadorAnimal qr = qr(EstadoIdentificador.ACTIVO, true, 0);
        when(animales.findById(animalId, company)).thenReturn(Optional.of(animal(EstadoAnimal.ACTIVO)));
        when(identificadores.findById(qr.id(), animalId, company)).thenReturn(Optional.of(qr));
        QrService.QrImageResult png = service.image(animalId, qr.id(), "png", 256);
        assertThat(png.contentType()).isEqualTo("image/png");
        assertThat(png.bytes()).isNotEmpty();
        assertThat(png.filename()).endsWith(".png");
        QrService.QrImageResult svg = service.image(animalId, qr.id(), "SVG", 512);
        assertThat(svg.contentType()).contains("image/svg+xml");
        assertThat(new String(svg.bytes(), java.nio.charset.StandardCharsets.UTF_8)).contains("<svg");
    }

    private Animal animal(EstadoAnimal state) {
        return new Animal(animalId, company, "A-1", null, SexoAnimal.HEMBRA, null, false,
                UUID.randomUUID(), UUID.randomUUID(), null, PropositoAnimal.CARNE, OrigenAnimal.NACIDO,
                property, UUID.randomUUID(), null, state, LocalDate.now(), null, null, null, null, null, 0);
    }

    private IdentificadorAnimal qr(EstadoIdentificador estado, boolean principal, long version) {
        return identificador(TipoIdentificador.QR, UUID.randomUUID().toString(), principal, estado, version);
    }

    private IdentificadorAnimal identificador(TipoIdentificador tipo, String valor, boolean principal,
                                              EstadoIdentificador estado, long version) {
        Instant now = Instant.now();
        UUID id = UUID.randomUUID();
        return new IdentificadorAnimal(id, company, animalId, tipo, valor, principal, estado,
                now, estado == EstadoIdentificador.RETIRADO ? now : null,
                estado == EstadoIdentificador.RETIRADO ? "prueba" : null,
                userId, estado == EstadoIdentificador.RETIRADO ? userId : null, null,
                tipo == TipoIdentificador.QR ? payloads.toJson(payloads.sign(animalId, id)) : null,
                now, now, version);
    }
}
