package com.example.householdbudgettracker.adapter.in.web.dto;

import java.math.BigDecimal;

public record AddIncomeRequest(String description, BigDecimal amount) {
}
