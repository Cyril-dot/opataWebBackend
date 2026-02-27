package com.beautyShop.Opata.Website.Exceptions;

import com.beautyShop.Opata.Website.entity.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;

/**
 * Global exception handler — converts all exceptions into clean ApiResult responses.
 * Works across AdminOrderController, AdminProductController, TelegramAdminController.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 Bad Request ───────────────────────────────────────

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResult<String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("⚠️ Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ApiResult.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<String>> handleValidationError(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("⚠️ Validation error: {}", message);
        return ResponseEntity.badRequest().body(ApiResult.error(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<String>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .findFirst()
                .orElse("Constraint violation");
        log.warn("⚠️ Constraint violation: {}", message);
        return ResponseEntity.badRequest().body(ApiResult.error(message));
    }

    // ── 403 Forbidden ─────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<String>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("🚫 Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResult.error("Access denied. Admin privileges required."));
    }

    // ── 404 Not Found ─────────────────────────────────────────

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResult<String>> handleRuntimeException(RuntimeException ex) {
        String msg = ex.getMessage();
        if (msg != null && (msg.contains("not found") || msg.contains("Not Found"))) {
            log.warn("🔍 Not found: {}", msg);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResult.error(msg));
        }
        log.error("❌ Runtime error: {}", msg, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("An unexpected error occurred: " + msg));
    }

    // ── 413 Payload Too Large ─────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResult<String>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("📁 File upload too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResult.error("File size exceeds the maximum allowed upload size."));
    }

    // ── 500 IO / General errors ───────────────────────────────

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResult<String>> handleIOException(IOException ex) {
        log.error("❌ IO error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("File operation failed: " + ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<String>> handleGenericException(Exception ex) {
        log.error("❌ Unhandled exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error("An internal server error occurred."));
    }
}