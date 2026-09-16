package com.ihya.api.dailypractice;

import com.ihya.api.common.web.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Daily-practice-module error handling, same shape as
 * {@code CatalogueExceptionHandler}/{@code IdentityExceptionHandler}.
 *
 * <ul>
 *   <li>{@link PracticeNotFoundException} &rarr; 404</li>
 *   <li>{@link ReplacementNotAvailableException} &rarr; 409</li>
 * </ul>
 *
 * <p>{@link com.ihya.api.catalogue.SunnahNotFoundException} (a client-supplied
 * {@code sunnahId} that doesn't exist in the catalogue) is already handled
 * globally by {@code CatalogueExceptionHandler} — Spring's exception
 * resolution matches by exception type across every {@code @RestControllerAdvice}
 * in the app, not by which module's controller threw it, so no duplicate
 * handling is needed here. An invalid pagination cursor throws
 * {@link IllegalArgumentException}, which stays with
 * {@code common.web.GlobalExceptionHandler} (&rarr; 400) as a module-agnostic
 * concern, same as the catalogue module's blank-field guards.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DailyPracticeExceptionHandler {

    @ExceptionHandler(PracticeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(PracticeNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ReplacementNotAvailableException.class)
    public ResponseEntity<ErrorResponse> handleReplacementNotAvailable(ReplacementNotAvailableException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
