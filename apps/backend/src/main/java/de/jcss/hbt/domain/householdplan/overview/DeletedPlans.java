package de.jcss.hbt.domain.householdplan.overview;

import org.jmolecules.architecture.cqrs.QueryModel;

import java.util.Comparator;
import java.util.List;

/** Read model of the deleted plans of a user: the newest year first, within a year the latest deleted first (GP-24). */
@QueryModel
public record DeletedPlans(List<DeletedPlanEntry> plans) {

    private static final Comparator<DeletedPlanEntry> ORDER = Comparator.comparing(DeletedPlanEntry::year)
            .thenComparing(DeletedPlanEntry::deletedAt)
            .reversed();

    public DeletedPlans {
        plans = plans.stream().sorted(ORDER).toList();
    }
}
