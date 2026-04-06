package com.procurementai.shared.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consistent error responses across all endpoints.
 * Procurement managers using the demo need clear error messages — not stack traces.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ── 400 Bad Request ────────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value"
            ));

        return error(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleFileTooLarge(MaxUploadSizeExceededException ex) {
        return error(HttpStatus.BAD_REQUEST,
            "File too large. Maximum upload size is 20MB.", null);
    }

    // ── 404 Not Found ──────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    // ── 403 Forbidden ──────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, "You don't have permission to access this resource.", null);
    }

    // ── 429 Rate Limit / Quota ─────────────────────────────────

    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ApiError> handleQuotaExceeded(QuotaExceededException ex) {
        return error(HttpStatus.TOO_MANY_REQUESTS,
            "Monthly document limit reached. Upgrade your plan to process more documents.", null);
    }

    // ── 500 Internal Server Error ──────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
            "Something went wrong. Our team has been notified.", null);
    }

    // ── Helper ─────────────────────────────────────────────────

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Object details) {
        return ResponseEntity.status(status)
            .body(new ApiError(status.value(), message, details, OffsetDateTime.now()));
    }
}
