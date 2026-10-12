package de.jcss.hbt.infrastructure.audit;

import de.jcss.hbt.domain.householdplan.event.BudgetItemRemoved;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.sql.Timestamp;

/**
 * Example of reacting to a domain event in a transaction of its own: records every
 * removed budget item in the {@code audit_log} table.
 * <p>
 * The listener only runs once the transaction that removed the item has committed, so
 * nothing is logged for a removal that is rolled back. Writing the log entry needs a new
 * transaction ({@link Propagation#REQUIRES_NEW}), because the original one is already
 * finished at that point. Should writing the entry fail, the removal itself stays in place.
 */
@Component
public class AuditLogListener {

    private final JdbcClient jdbcClient;

    public AuditLogListener(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(BudgetItemRemoved event) {
        jdbcClient.sql("""
                        INSERT INTO audit_log (event_type, household_plan_id, budget_item_id, occurred_at)
                        VALUES (:eventType, :planId, :itemId, :occurredAt)
                        """)
                .param("eventType", BudgetItemRemoved.class.getSimpleName())
                .param("planId", event.planId().value())
                .param("itemId", event.itemId().value())
                .param("occurredAt", Timestamp.from(event.occurredAt()))
                .update();
    }
}
