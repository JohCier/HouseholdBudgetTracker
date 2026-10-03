package com.example.householdbudgettracker.adapter.in.web;

import com.example.householdbudgettracker.domain.exception.BudgetItemNotFoundException;
import com.example.householdbudgettracker.domain.exception.InvalidBudgetItemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translates domain exceptions raised by the application layer into HTTP responses,
 * keeping the domain itself free of any web/HTTP concerns.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidBudgetItemException.class)
    public ResponseEntity<Map<String, String>> handleInvalidBudgetItem(InvalidBudgetItemException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(BudgetItemNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleBudgetItemNotFound(BudgetItemNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }
}
