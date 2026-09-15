package com.ihya.api.catalogue;

import com.ihya.api.common.web.ErrorResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Catalogue-module error handling. Turns the exceptions the catalogue services
 * raise into the shared {@link ErrorResponse} JSON shape. Lives in
 * {@code catalogue} (not {@code common.web}) so shared code carries no
 * dependency on this module.
 *
 * <ul>
 *   <li>{@link CategoryNotFoundException} &rarr; 404</li>
 *   <li>{@link SunnahNotFoundException} &rarr; 404</li>
 *   <li>{@link CategoryNameAlreadyExistsException} &rarr; 409</li>
 *   <li>{@link CategorySlugAlreadyExistsException} &rarr; 409</li>
 *   <li>{@link SunnahSlugAlreadyExistsException} &rarr; 409</li>
 * </ul>
 *
 * <p>Blank-field guards in the catalogue services throw
 * {@link IllegalArgumentException}, which stays with
 * {@code common.web.GlobalExceptionHandler} (&rarr; 400) as a module-agnostic
 * concern. A write attempted without the {@code ADMIN} role is rejected by
 * Spring Security's own {@code @PreAuthorize} enforcement (&rarr; 403) before a
 * request ever reaches a controller method, so there is nothing to handle here.
 *
 * <p>{@code @Order(HIGHEST_PRECEDENCE)} so these specific handlers are consulted
 * before {@code common.web.GlobalExceptionHandler}'s {@code Exception} catch-all.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CatalogueExceptionHandler {

    @ExceptionHandler({CategoryNotFoundException.class, SunnahNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({CategoryNameAlreadyExistsException.class, CategorySlugAlreadyExistsException.class,
            SunnahSlugAlreadyExistsException.class})
    public ResponseEntity<ErrorResponse> handleAlreadyExists(RuntimeException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ErrorResponse.of(status, message));
    }
}
