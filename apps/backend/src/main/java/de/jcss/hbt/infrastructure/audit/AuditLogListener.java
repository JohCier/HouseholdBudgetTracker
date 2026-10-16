package de.jcss.hbt.infrastructure.audit;

import de.jcss.hbt.domain.householdplan.event.HouseholdPlanEvent;
import de.jcss.hbt.domain.shared.TimestampedDomainEvent;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.RecordComponent;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Records every domain event in the {@code audit_log} table, in a transaction of its own.
 * <p>
 * The listener subscribes to {@link TimestampedDomainEvent}, the common type of all events,
 * so new events are logged without changes here. Each entry keeps the event's type, when it
 * happened and the full event as JSON; events about a {@link HouseholdPlanEvent household plan}
 * also fill the plan column, so the log can be filtered by plan.
 * <p>
 * The listener only runs once the transaction that published the event has committed, so
 * nothing is logged for a change that is rolled back. Writing the log entry needs a new
 * transaction ({@link Propagation#REQUIRES_NEW}), because the original one is already
 * finished at that point. Should writing the entry fail, the change itself stays in place.
 */
@Component
public class AuditLogListener {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    public AuditLogListener(JdbcClient jdbcClient, JsonMapper jsonMapper) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(TimestampedDomainEvent event) {
        jdbcClient.sql("""
                        INSERT INTO audit_log (event_type, household_plan_id, payload, occurred_at)
                        VALUES (:eventType, :planId, CAST(:payload AS JSONB), :occurredAt)
                        """)
                .param("eventType", event.getClass().getSimpleName())
                .param("planId", planIdOf(event))
                .param("payload", jsonMapper.writeValueAsString(componentsOf((Record) event))) // events are records, see ArchitectureTest
                .param("occurredAt", Timestamp.from(event.timestamp()))
                .update();
    }

    private static UUID planIdOf(TimestampedDomainEvent event) {
        return event instanceof HouseholdPlanEvent planEvent ? planEvent.planId().value() : null;
    }

    /**
     * Turns a record into a map of its components, recursively. Nested records with a single
     * component, like typed ids, are reduced to that component, so the JSON reads
     * {@code "planId": "…"} instead of {@code "planId": {"value": "…"}}.
     */
    private static Map<String, Object> componentsOf(Record record) {
        Map<String, Object> json = new LinkedHashMap<>();
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            json.put(component.getName(), toJson(valueOf(component, record)));
        }
        return json;
    }

    private static Object toJson(Object value) {
        if (!(value instanceof Record record)) {
            return value;
        }
        RecordComponent[] components = record.getClass().getRecordComponents();
        return components.length == 1 ? toJson(valueOf(components[0], record)) : componentsOf(record);
    }

    private static Object valueOf(RecordComponent component, Record record) {
        try {
            return component.getAccessor().invoke(record);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read " + component.getName() + " of " + record, e);
        }
    }
}
