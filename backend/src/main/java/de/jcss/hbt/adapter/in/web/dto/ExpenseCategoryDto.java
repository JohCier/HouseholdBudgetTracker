package de.jcss.hbt.adapter.in.web.dto;

import de.jcss.hbt.domain.ExpenseCategory;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * API representation of {@link ExpenseCategory}. Kept separate so that renaming a
 * domain constant cannot silently change the REST contract; the exhaustive switches
 * fail to compile when the two drift apart.
 */
@Schema(name = "ExpenseCategory", enumAsRef = true)
public enum ExpenseCategoryDto {
    LEBENSUNTERHALT,
    FREIZEIT_SPASS,
    SPAREN_INVESTIEREN;

    public static ExpenseCategoryDto from(ExpenseCategory category) {
        return switch (category) {
            case LEBENSUNTERHALT -> LEBENSUNTERHALT;
            case FREIZEIT_SPASS -> FREIZEIT_SPASS;
            case SPAREN_INVESTIEREN -> SPAREN_INVESTIEREN;
        };
    }

    public ExpenseCategory toDomain() {
        return switch (this) {
            case LEBENSUNTERHALT -> ExpenseCategory.LEBENSUNTERHALT;
            case FREIZEIT_SPASS -> ExpenseCategory.FREIZEIT_SPASS;
            case SPAREN_INVESTIEREN -> ExpenseCategory.SPAREN_INVESTIEREN;
        };
    }
}
