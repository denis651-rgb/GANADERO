package bo.com.ganadero.shared.web;

import bo.com.ganadero.shared.api.ApiError;
import bo.com.ganadero.shared.error.BusinessException;
import bo.com.ganadero.shared.error.ErrorCode;
import bo.com.ganadero.shared.error.SyncConflictException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> handleBusiness(BusinessException exception, HttpServletRequest request) {
        return error(exception.code(), exception.getMessage(), request);
    }

    @ExceptionHandler(SyncConflictException.class)
    ResponseEntity<ApiError> handleSyncConflict(SyncConflictException exception, HttpServletRequest request) {
        ApiError body = new ApiError(false, exception.code().name(), exception.getMessage(), List.of(),
                Instant.now(), correlationId(request), exception.localVersion(), exception.serverVersion(),
                exception.serverData(), exception.conflictingFields(), exception.suggestedAction());
        return ResponseEntity.status(exception.code().status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        List<ApiError.FieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage())).toList();
        ApiError body = new ApiError(false, ErrorCode.VALIDATION_ERROR.name(),
                ErrorCode.VALIDATION_ERROR.defaultMessage(), fields, Instant.now(), correlationId(request));
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException exception,
                                               HttpServletRequest request) {
        return error(ErrorCode.VALIDATION_ERROR, "El cuerpo de la solicitud no es un JSON válido.", request);
    }

    @ExceptionHandler({HandlerMethodValidationException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> handleParameterValidation(Exception exception, HttpServletRequest request) {
        return error(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), request);
    }

    // Ej.: un cliente pide GET /api/v1/x/{id} con un id que no es UUID válido. Es un error del
    // cliente (400), no una falla del servidor — sin este handler caía en handleUnexpected (500)
    // y el frontend lo reintentaba indefinidamente creyendo que era un problema transitorio.
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        LOGGER.warn("Parámetro inválido '{}'={} en {} {}. correlationId={}", exception.getName(), exception.getValue(),
                request.getMethod(), request.getRequestURI(), correlationId(request));
        return error(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleVersionConflict(HttpServletRequest request) {
        return error(ErrorCode.VERSION_CONFLICT, ErrorCode.VERSION_CONFLICT.defaultMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        LOGGER.warn("Data integrity violation. correlationId={}", correlationId(request), exception);
        return error(ErrorCode.BUSINESS_RULE_VIOLATION,
                "La operación viola una restricción de integridad.", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unexpected error in {} {}. correlationId={}", request.getMethod(), request.getRequestURI(),
                correlationId(request), exception);
        return error(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), request);
    }

    private ResponseEntity<ApiError> error(ErrorCode code, String message, HttpServletRequest request) {
        return ResponseEntity.status(code.status()).body(new ApiError(false, code.name(), message,
                List.of(), Instant.now(), correlationId(request)));
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return value == null ? "unknown" : value.toString();
    }
}
