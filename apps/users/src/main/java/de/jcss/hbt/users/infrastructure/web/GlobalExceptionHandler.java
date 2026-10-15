package de.jcss.hbt.users.infrastructure.web;

import de.jcss.hbt.users.domain.exception.DomainValidationException;
import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Renders all errors as RFC 9457 problem details ({@code application/problem+json}).
 * Framework exceptions (malformed JSON, ...) are covered by {@link ResponseEntityExceptionHandler};
 * domain exceptions are translated here, keeping the domain itself free of any web/HTTP
 * concerns. The human-readable message is always in {@code detail}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmailAddressAlreadyRegisteredException.class)
    public ProblemDetail handleAlreadyRegistered(EmailAddressAlreadyRegisteredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Lists every invalid field in {@code detail} and as an {@code errors} map. */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, this::messageOf, (first, _) -> first, TreeMap::new));
        ProblemDetail body = ex.getBody();
        body.setDetail(errors.entrySet().stream()
                .map(error -> error.getKey() + ": " + error.getValue())
                .collect(Collectors.joining(", ")));
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private String messageOf(FieldError error) {
        return error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage();
    }
}
