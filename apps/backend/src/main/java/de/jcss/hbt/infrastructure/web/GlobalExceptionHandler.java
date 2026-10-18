package de.jcss.hbt.infrastructure.web;

import de.jcss.hbt.domain.exception.BudgetItemNotFoundException;
import de.jcss.hbt.domain.exception.DomainValidationException;
import de.jcss.hbt.domain.exception.HouseholdPlanAlreadyExistsException;
import de.jcss.hbt.domain.exception.HouseholdPlanNotFoundException;
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
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Renders all errors as RFC 9457 problem details ({@code application/problem+json}).
 * Framework exceptions (malformed JSON, missing parameters, ...) are covered by
 * {@link ResponseEntityExceptionHandler}; domain exceptions are translated here, keeping
 * the domain itself free of any web/HTTP concerns. The human-readable message is always
 * in {@code detail} (English, for logs and API clients). Each domain error also carries a stable
 * {@code type}, by which the frontend shows its own German message (FM-2).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final URI INVALID_INPUT = URI.create("urn:hbt:invalid-input");
    static final URI PLAN_NOT_FOUND = URI.create("urn:hbt:plan-not-found");
    static final URI ITEM_NOT_FOUND = URI.create("urn:hbt:item-not-found");
    static final URI PLAN_ALREADY_EXISTS = URI.create("urn:hbt:plan-already-exists");

    @ExceptionHandler(DomainValidationException.class)
    public ProblemDetail handleDomainValidation(DomainValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_INPUT, ex);
    }

    @ExceptionHandler(HouseholdPlanNotFoundException.class)
    public ProblemDetail handlePlanNotFound(HouseholdPlanNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, PLAN_NOT_FOUND, ex);
    }

    @ExceptionHandler(BudgetItemNotFoundException.class)
    public ProblemDetail handleItemNotFound(BudgetItemNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ITEM_NOT_FOUND, ex);
    }

    @ExceptionHandler(HouseholdPlanAlreadyExistsException.class)
    public ProblemDetail handleHouseholdPlanAlreadyExists(HouseholdPlanAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, PLAN_ALREADY_EXISTS, ex);
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

    /** Invalid request parameters, such as a year out of range, are invalid input as well. */
    @Override
    protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = ex.getBody();
        body.setType(INVALID_INPUT);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    private String messageOf(FieldError error) {
        return error.getDefaultMessage() == null ? "invalid" : error.getDefaultMessage();
    }
}
