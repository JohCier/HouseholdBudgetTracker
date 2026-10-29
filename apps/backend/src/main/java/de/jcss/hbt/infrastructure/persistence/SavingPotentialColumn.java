package de.jcss.hbt.infrastructure.persistence;

import de.jcss.hbt.domain.householdplan.BudgetItem;
import de.jcss.hbt.domain.householdplan.valueobject.Money;
import de.jcss.hbt.domain.householdplan.valueobject.SavingPotential;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps the {@link SavingPotential} of an item to the column {@code budget_item.saving_potential}
 * (monthly, {@code NULL} if none) and back.
 */
final class SavingPotentialColumn {

    private SavingPotentialColumn() {
    }

    /** @return {@code null} if the item has none */
    static SavingPotential of(ResultSet rs) throws SQLException {
        BigDecimal savingPotential = rs.getBigDecimal("saving_potential");
        return savingPotential == null ? null : SavingPotential.monthly(Money.of(savingPotential));
    }

    static BigDecimal valueOf(BudgetItem item) {
        return item.savingPotential().map(savingPotential -> savingPotential.monthly().value()).orElse(null);
    }
}
