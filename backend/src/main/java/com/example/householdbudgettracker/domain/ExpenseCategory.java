package com.example.householdbudgettracker.domain;

/**
 * Categories an expense item can be assigned to, carrying the target share of
 * income defined by the 50/30/20 budgeting rule.
 */
public enum ExpenseCategory {
    LEBENSUNTERHALT(50),
    FREIZEIT_SPASS(30),
    SPAREN_INVESTIEREN(20);

    private final int targetPercentageOfIncome;

    ExpenseCategory(int targetPercentageOfIncome) {
        this.targetPercentageOfIncome = targetPercentageOfIncome;
    }

    public int targetPercentageOfIncome() {
        return targetPercentageOfIncome;
    }
}
