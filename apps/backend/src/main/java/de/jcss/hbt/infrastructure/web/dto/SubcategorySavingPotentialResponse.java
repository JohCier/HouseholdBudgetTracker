package de.jcss.hbt.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.jcss.hbt.domain.householdplan.overview.SubcategorySavingPotential;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/** The saving potential of all expenses of one subcategory, across the categories (SU-1, SU-2). */
public record SubcategorySavingPotentialResponse(
        @Schema(example = "Abonnements", description = "\"Sonstiges\" for the expenses without a subcategory")
        String subcategory,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "15.00",
                description = "The sum of the monthly saving potentials of its expenses")
        BigDecimal monthlyAmount,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(implementation = String.class, format = "decimal", example = "180.00",
                description = "Twelve times the monthly amount")
        BigDecimal amount
) {

    public static SubcategorySavingPotentialResponse from(SubcategorySavingPotential sum) {
        return new SubcategorySavingPotentialResponse(sum.subcategory().value(), sum.monthly().value(),
                sum.annual().value());
    }
}
