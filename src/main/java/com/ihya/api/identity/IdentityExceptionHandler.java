package com.ihya.api.identity;

import com.ihya.api.common.web.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Identity-module error handling. Turns the exceptions the identity endpoints
 * raise into the shared {@link ErrorResponse} JSON shape, so a client never sees
 * a raw stack trace or an internal exception type. Lives in {@code identity}
 * (not {@code common.web}) so shared code carries no dependency on this module.
 *
 * <ul>
 *   <li>{@link EmailAlreadyRegisteredException} &rarr; 409</li>
 *   <li>{@link InvalidCredentialsException} &rarr; 401</li>
 *   <li>{@link InvalidRefreshTokenException} &rarr; 401 (covers the reuse-detection
 *       path documented in {@code openapi/identity-api.yaml})</li>
 *   <li>{@link UserNotFoundException} &rarr; 401 (verified token whose user was
 *       since deleted)</li>
 * </ul>
 *
 * <p>Generic request-shape failures ({@code @Valid} aggregation, malformed JSON,
 * unsupported media type) are module-agnostic and handled by
 * {@code common.web.GlobalExceptionHandler}.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} so these specific handlers are consulted
 * before {@code common.web.GlobalExceptionHandler}'s {@code Exception} catch-all.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IdentityExceptionHandler {

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
     * The id in a verified access token no longer matches a user row (account
     * deleted after issue). Treated as an authentication failure, not a 404 —
     * see {@link UserNotFoundException}.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
