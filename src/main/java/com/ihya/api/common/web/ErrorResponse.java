package com.ihya.api.common.web;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

/**
 * Wire contract for every error emitted by the API — identity and catalogue
 * alike. An HTTP {@code status} code, its {@code error} reason phrase, a
 * human-readable {@code message} and the {@code timestamp} the response was
 * produced.
 *
 * <p>Built only via {@link #of}: by {@link GlobalExceptionHandler} and
 * {@link RestAuthenticationEntryPoint} here, and by the per-module advice classes
 * ({@code IdentityExceptionHandler}, {@code CatalogueExceptionHandler}) in their
 * own packages. That factory is the single definition of what an error body
 * looks like. Nothing here ever carries a stack trace or an internal exception
 * type.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        OffsetDateTime timestamp) {

    public static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, OffsetDateTime.now());
    }
}
