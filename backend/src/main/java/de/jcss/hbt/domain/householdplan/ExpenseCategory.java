package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.shared.ValueObject;

/**
 * Categories an expense item can be assigned to, carrying the target share of income
 * defined by the 50/30/20 budgeting rule: at most 50 % for living costs, at most 30 %
 * for leisure, and at least 20 % for savings.
 */
@ValueObject
public enum ExpenseCategory {
    LEBENSUNTERHALT(50, TargetDirection.AT_MOST),
    FREIZEIT_SPASS(30, TargetDirection.AT_MOST),
    SPAREN_INVESTIEREN(20, TargetDirection.AT_LEAST);

    private final int targetPercentageOfIncome;
    private final TargetDirection targetDirection;

    ExpenseCategory(int targetPercentageOfIncome, TargetDirection targetDirection) {
        this.targetPercentageOfIncome = targetPercentageOfIncome;
        this.targetDirection = targetDirection;
    }

    public int targetPercentageOfIncome() {
        return targetPercentageOfIncome;
    }

    public TargetDirection targetDirection() {
        return targetDirection;
    }
}
