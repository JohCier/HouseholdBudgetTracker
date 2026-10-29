package de.jcss.hbt.domain.householdplan.overview;

import de.jcss.hbt.domain.householdplan.valueobject.BudgetSummary;
import de.jcss.hbt.domain.householdplan.valueobject.HouseholdPlanId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Year;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeletedPlansTest {

    /** GP-24 */
    @Test
    void theNewestYearComesFirstAndWithinAYearTheLatestDeleted() {
        DeletedPlanEntry earlier2026 = entry(2026, "2026-10-20T18:42:00Z");
        DeletedPlanEntry later2026 = entry(2026, "2026-10-29T20:15:00Z");
        DeletedPlanEntry of2023 = entry(2023, "2026-11-02T09:05:00Z");

        DeletedPlans deleted = new DeletedPlans(List.of(of2023, earlier2026, later2026));

        assertThat(deleted.plans()).containsExactly(later2026, earlier2026, of2023);
    }

    private static DeletedPlanEntry entry(int year, String deletedAt) {
        return DeletedPlanEntry.of(HouseholdPlanId.generate(), Year.of(year), Instant.parse(deletedAt), 0,
                BudgetSummary.EMPTY);
    }
}
