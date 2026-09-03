package com.ihya.api.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Translates every exception that escapes a controller into a clean
 * {@link ErrorResponse} JSON body, so a client never sees a raw stack trace or
 * an internal exception type.
 *
 * <ul>
 *   <li>{@link EmailAlreadyRegisteredException} &rarr; 409</li>
 *   <li>{@link InvalidCredentialsException} &rarr; 401</li>
 *   <li>{@link InvalidRefreshTokenException} &rarr; 401 (covers the reuse-detection
 *       path documented in {@code openapi/identity-api.yaml})</li>
 *   <li>{@link MethodArgumentNotValidException} (a {@code @Valid} failure) &rarr; 400,
 *       with the actual field-level messages flattened into {@code message}</li>
 *   <li>anything else &rarr; 500 with a fixed generic message; the real exception
 *       (with stack trace) is logged server-side and never sent to the client</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(EmailAlreadyRegisteredException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Bean Validation failure on an {@code @Valid} request body. Replaces
     * Spring's verbose default payload with just the field-level messages,
     * e.g. {@code "password: password must be at least 8 characters"}.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::formatFieldError)
                .sorted()
                .distinct()
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = "Request validation failed";
        }
        return build(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Catch-all for anything not handled above. The client gets a fixed generic
     * message; the real cause and its stack trace go to the server log only.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception reached the controller advice", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private static String formatFieldError(FieldError fieldError) {
        String detail = fieldError.getDefaultMessage();
        return fieldError.getField() + ": " + (detail == null ? "invalid value" : detail);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
