package de.jcss.hbt.users.infrastructure.web;

import de.jcss.hbt.users.domain.exception.DomainValidationException;
import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.exception.LoginLockedException;
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

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Renders all errors as RFC 9457 problem details ({@code application/problem+json}).
 * Framework exceptions (malformed JSON, ...) are covered by {@link ResponseEntityExceptionHandler};
 * domain exceptions are translated here, keeping the domain itself free of any web/HTTP
 * concerns. The human-readable message is always in {@code detail} (English, for logs and API
 * clients). Each domain error also carries a stable {@code type}, by which the frontend shows its
 * own German message (FM-2).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final URI INVALID_INPUT = URI.create("urn:hbt:invalid-input");
    static final URI EMAIL_ALREADY_REGISTERED = URI.create("urn:hbt:email-already-registered");
    static final URI INVALID_CREDENTIALS = URI.create("urn:hbt:invalid-credentials");
    static final URI TOO_MANY_LOGIN_ATTEMPTS = URI.create("urn:hbt:too-many-login-attempts");

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_INPUT, ex);
    }

    @ExceptionHandler(EmailAddressAlreadyRegisteredException.class)
    public ProblemDetail handleAlreadyRegistered(EmailAddressAlreadyRegisteredException ex) {
        return problem(HttpStatus.CONFLICT, EMAIL_ALREADY_REGISTERED, ex);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS, ex);
    }

    @ExceptionHandler(LoginLockedException.class)
    public ProblemDetail handleLoginLocked(LoginLockedException ex) {
        return problem(HttpStatus.TOO_MANY_REQUESTS, TOO_MANY_LOGIN_ATTEMPTS, ex);
    }

    private static ProblemDetail problem(HttpStatus status, URI type, RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        problem.setType(type);
        return problem;
    }

    /** Lists every invalid field in {@code detail} and as an {@code errors} map. */
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, this::messageOf, (first, _) -> first, TreeMap::new));
        ProblemDetail body = ex.getBody();
        body.setType(INVALID_INPUT);
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
