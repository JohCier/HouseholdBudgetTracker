package com.example.householdbudgettracker.adapter.in.web.dto;

import com.example.householdbudgettracker.domain.ExpenseCategory;

import java.math.BigDecimal;

public record AddExpenseRequest(String description, BigDecimal amount, ExpenseCategory category) {
}
