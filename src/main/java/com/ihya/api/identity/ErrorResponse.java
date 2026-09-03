package com.ihya.api.identity;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;

/**
 * Wire contract for every error emitted by the identity endpoints.
 *
 * <p>Mirrors the {@code ErrorResponse} schema in
 * {@code openapi/identity-api.yaml}: an HTTP {@code status} code, its
 * {@code error} reason phrase, a human-readable {@code message} and the
 * {@code timestamp} the response was produced.
 *
 * <p>Populated exclusively by {@link GlobalExceptionHandler}; nothing here ever
 * carries a stack trace or an internal exception type.
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        OffsetDateTime timestamp) {

    static ErrorResponse of(HttpStatus status, String message) {
        return new ErrorResponse(status.value(), status.getReasonPhrase(), message, OffsetDateTime.now());
    }
}
