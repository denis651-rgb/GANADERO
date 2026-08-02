package bo.com.ganadero.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos."),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "La solicitud requiere autenticación."),
    EMPRESA_NOT_FOUND(HttpStatus.NOT_FOUND, "La empresa no existe."),
    MEMBERSHIP_NOT_FOUND(HttpStatus.FORBIDDEN, "El usuario no posee una membresía activa."),
    USER_NOT_AUTHORIZED(HttpStatus.FORBIDDEN, "El usuario no tiene permiso para realizar esta operación."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "El usuario no existe en la empresa."),
    USER_ALREADY_MEMBER(HttpStatus.CONFLICT, "El usuario ya pertenece a la empresa."),
    BOOTSTRAP_DISABLED(HttpStatus.NOT_FOUND, "El recurso no existe."),
    BOOTSTRAP_TOKEN_INVALID(HttpStatus.FORBIDDEN, "No autorizado."),
    BOOTSTRAP_ALREADY_COMPLETED(HttpStatus.CONFLICT, "La empresa inicial ya fue creada."),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "Idempotency-Key es obligatorio."),
    IDEMPOTENCY_CONFLICT(HttpStatus.CONFLICT, "La clave de idempotencia ya fue usada con otra solicitud."),
    SUPABASE_AUTH_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "El servicio de identidad no está disponible."),
    STORAGE_FILE_INVALID(HttpStatus.UNPROCESSABLE_CONTENT, "El archivo no cumple la política de seguridad."),
    SYSTEM_STATUS_DISABLED(HttpStatus.NOT_FOUND, "El recurso no existe."),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "El rol no existe o no está disponible para la empresa."),
    ROLE_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Ya existe un rol con ese código."),
    LAST_ACTIVE_OWNER(HttpStatus.UNPROCESSABLE_CONTENT, "No se puede bloquear o degradar al último propietario activo."),
    SYSTEM_ROLE_IMMUTABLE(HttpStatus.UNPROCESSABLE_CONTENT, "Los datos estructurales de un rol del sistema no pueden modificarse."),
    PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "La propiedad no existe."),
    PROPERTY_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Ya existe una propiedad con ese código."),
    PROPERTY_HAS_ACTIVE_ANIMALS(HttpStatus.UNPROCESSABLE_CONTENT, "La propiedad contiene animales activos y no puede desactivarse."),
    PROPERTY_ACCESS_DENIED(HttpStatus.FORBIDDEN, "El usuario no tiene acceso a la propiedad."),
    SECTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "El sector no existe."),
    SECTOR_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Ya existe un sector con ese código en la propiedad."),
    GRASS_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "El tipo de pasto no existe."),
    PADDOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "El potrero no existe."),
    PADDOCK_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Ya existe un potrero con ese código en la propiedad."),
    PADDOCK_HAS_ACTIVE_ANIMALS(HttpStatus.UNPROCESSABLE_CONTENT, "El potrero contiene animales activos y no puede desactivarse."),
    ANIMAL_NOT_FOUND(HttpStatus.NOT_FOUND, "El animal no existe."),
    ANIMAL_CODE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Ya existe un animal con ese código."),
    BREED_NOT_FOUND(HttpStatus.NOT_FOUND, "La raza no existe."),
    ANIMAL_CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "La categoría del animal no existe."),
    ANIMAL_CATEGORY_SEX_MISMATCH(HttpStatus.UNPROCESSABLE_CONTENT, "La categoría no corresponde al sexo del animal."),
    INVALID_ANIMAL_LOCATION(HttpStatus.UNPROCESSABLE_CONTENT, "El potrero no pertenece a la propiedad indicada."),
    INVALID_ANIMAL_STATE_TRANSITION(HttpStatus.UNPROCESSABLE_CONTENT, "El cambio de estado del animal no está permitido."),
    IDENTIFIER_ALREADY_EXISTS(HttpStatus.CONFLICT, "El identificador ya se encuentra registrado."),
    ANIMAL_NOT_ACTIVE(HttpStatus.UNPROCESSABLE_CONTENT, "El animal no se encuentra activo."),
    LOT_NOT_FOUND(HttpStatus.NOT_FOUND, "El lote no existe."),
    ANIMAL_ALREADY_IN_ACTIVE_LOT(HttpStatus.CONFLICT, "El animal ya pertenece a un lote activo."),
    INVALID_MOVEMENT_ORIGIN(HttpStatus.UNPROCESSABLE_CONTENT, "El origen del movimiento no coincide con la ubicación actual."),
    MOVEMENT_ALREADY_CONFIRMED(HttpStatus.CONFLICT, "El movimiento ya fue confirmado."),
    VERSION_CONFLICT(HttpStatus.CONFLICT, "El registro fue modificado por otro usuario."),
    BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, "La operación incumple una regla de negocio."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error interno.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus status() { return status; }
    public String defaultMessage() { return defaultMessage; }
}
