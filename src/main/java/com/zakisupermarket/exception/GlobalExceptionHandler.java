package com.zakisupermarket.exception;

import com.zakisupermarket.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // LocalizedException (and everything that extends it - FeatureDisabledException,
    // AccountLockedException, SessionExpiredException, MaxExtensionsReachedException,
    // ResourceNotFoundException) carries its own HTTP status + a stable error `code` +
    // interpolation `params`, so this single handler replaces what used to be 5
    // separate handlers producing 3 different JSON shapes. The frontend resolves
    // `code` to ERRORS.<code> in ar.json/en.json instead of showing `message` (English,
    // kept only for server logs and as a fallback for any not-yet-migrated caller).
    @ExceptionHandler(LocalizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleLocalizedException(LocalizedException ex) {
        log.warn("{} [{}]: {}", ex.getStatus(), ex.getErrorCode(), ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(ex.getMessage(), ex.getErrorCode(), ex.getParams(), ex.getStatus().value());
        return new ResponseEntity<>(body, ex.getStatus());
    }

    // Case-insensitive fragments of a RuntimeException's message that indicate what
    // HTTP status it actually corresponds to. This only applies to a plain
    // `new RuntimeException("...")` that hasn't been migrated to LocalizedException yet -
    // its message reaches the client unchanged (untranslated), exactly as before.
    private static final java.util.List<String> NOT_FOUND_HINTS = java.util.List.of("not found", "no such", "does not exist");
    private static final java.util.List<String> CONFLICT_HINTS = java.util.List.of(
            "already exists", "already registered", "already in use", "duplicate");
    private static final java.util.List<String> FORBIDDEN_HINTS = java.util.List.of("access denied", "not authorized", "permission");

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        HttpStatus status = inferStatus(message);
        log.error("{}: {}", status, message);
        ApiResponse<Void> body = ApiResponse.error(message, status.value());
        return new ResponseEntity<>(body, status);
    }

    private HttpStatus inferStatus(String message) {
        if (message == null) {
            return HttpStatus.BAD_REQUEST;
        }
        String lower = message.toLowerCase();
        if (NOT_FOUND_HINTS.stream().anyMatch(lower::contains)) {
            return HttpStatus.NOT_FOUND;
        }
        if (CONFLICT_HINTS.stream().anyMatch(lower::contains)) {
            return HttpStatus.CONFLICT;
        }
        if (FORBIDDEN_HINTS.stream().anyMatch(lower::contains)) {
            return HttpStatus.FORBIDDEN;
        }
        return HttpStatus.BAD_REQUEST;
    }

    /**
     * Constraint violations (e.g. a race on a unique username/barcode/license number)
     * carry raw driver/SQL text in their message - never return that to the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage(), ex);
        ApiResponse<Void> body = ApiResponse.error(
                "This operation conflicts with existing data (e.g. a value that must be unique is already in use).",
                "DATA_CONFLICT", Map.of(), HttpStatus.CONFLICT.value());
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * A NullPointerException is always a bug, never a business rule - its message
     * ("Cannot invoke X because Y is null") is meaningless and potentially revealing
     * to an end user, so it gets the same generic treatment as the Exception fallback.
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointerException(NullPointerException ex) {
        log.error("Unexpected null pointer", ex);
        ApiResponse<Void> body = ApiResponse.error(
                "Internal server error", "INTERNAL_ERROR", Map.of(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException ex) {
        log.error("Access Denied: {}", ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                "Access denied", "ACCESS_DENIED", Map.of(), HttpStatus.FORBIDDEN.value());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.error("Bad Credentials: {}", ex.getMessage());
        ApiResponse<Void> body = ApiResponse.error(
                "Invalid credentials", "INVALID_CREDENTIALS", Map.of(), HttpStatus.UNAUTHORIZED.value());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        ApiResponse<Void> body = ApiResponse.error(
                "Missing required parameter: " + ex.getParameterName(),
                "MISSING_PARAMETER", Map.of("parameter", ex.getParameterName()), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingHeader(MissingRequestHeaderException ex) {
        // Same "access denied" treatment as a wrong key, not a generic 400 - a missing
        // X-Platform-Admin-Key should look identical to a wrong one to the caller.
        ApiResponse<Void> body = ApiResponse.error(
                "Access denied", "ACCESS_DENIED", Map.of(), HttpStatus.FORBIDDEN.value());
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Generic Exception: {}", ex.getMessage(), ex);
        ApiResponse<Void> body = ApiResponse.error(
                "Internal server error", "INTERNAL_ERROR", Map.of(), HttpStatus.INTERNAL_SERVER_ERROR.value());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
