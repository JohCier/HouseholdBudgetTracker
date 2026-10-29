package de.jcss.hbt.domain.householdplan.valueobject;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Categories an expense item can be assigned to, carrying the target share of income
 * defined by the 50/30/20 budgeting rule: at most 50 % for living costs, at most 30 %
 * for leisure, and at least 20 % for savings.
 */
@ValueObject
public enum ExpenseCategory {
    LEBENSUNTERHALT(Percentage.of(50), TargetDirection.AT_MOST),
    FREIZEIT_SPASS(Percentage.of(30), TargetDirection.AT_MOST),
    SPAREN_INVESTIEREN(Percentage.of(20), TargetDirection.AT_LEAST);

    private final Percentage targetPercentageOfIncome;
    private final TargetDirection targetDirection;

    ExpenseCategory(Percentage targetPercentageOfIncome, TargetDirection targetDirection) {
        this.targetPercentageOfIncome = targetPercentageOfIncome;
        this.targetDirection = targetDirection;
    }

    public Percentage targetPercentageOfIncome() {
        return targetPercentageOfIncome;
    }

    public TargetDirection targetDirection() {
        return targetDirection;
    }

    /**
     * Whether spending {@code categoryTotal} in this category keeps to its target share of
     * {@code totalIncome}. Without income no share can be computed: spending anything then
     * breaks a ceiling, while a savings floor cannot be missed.
     */
    public boolean isTargetMet(Money categoryTotal, Money totalIncome) {
        if (totalIncome.isZero()) {
            return targetDirection == TargetDirection.AT_LEAST || categoryTotal.isZero();
        }
        return targetDirection.isMet(Percentage.shareOf(categoryTotal, totalIncome), targetPercentageOfIncome);
    }
}
