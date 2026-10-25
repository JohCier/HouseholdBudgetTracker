package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.valueobject.ItemMarks;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps the {@link ItemMarks} of an item to the columns {@code budget_item.important} and
 * {@code budget_item.saving_potential} (monthly, {@code NULL} if none) and back.
 */
final class ItemMarksColumns {

    private ItemMarksColumns() {
    }

    static ItemMarks of(ResultSet rs) throws SQLException {
        BigDecimal savingPotential = rs.getBigDecimal("saving_potential");
        return new ItemMarks(rs.getBoolean("important"),
                savingPotential == null ? null : SavingPotential.monthly(Money.of(savingPotential)));
    }

    static BigDecimal savingPotentialOf(ItemMarks marks) {
        return marks.savingPotential() == null ? null : marks.savingPotential().monthly().value();
    }
}
