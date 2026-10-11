package de.jcss.hbt.domain.householdplan;

import de.jcss.hbt.domain.shared.ValueObject;

import java.math.BigDecimal;

/**
 * Whether the target share of an {@link ExpenseCategory} is a ceiling (spend at most)
 * or a floor (put aside at least).
 */
@ValueObject
public enum TargetDirection {
    AT_MOST,
    AT_LEAST;

    public boolean isMet(BigDecimal actualPercentage, int targetPercentage) {
        int comparison = actualPercentage.compareTo(BigDecimal.valueOf(targetPercentage));
        return this == AT_MOST ? comparison <= 0 : comparison >= 0;
    }
}
