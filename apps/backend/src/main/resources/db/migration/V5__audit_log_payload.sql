-- The audit log records every domain event, not only removed items: the full event is
-- kept as JSON, the plan it is about stays a column of its own for filtering.
ALTER TABLE audit_log
    ADD COLUMN payload JSONB;
UPDATE audit_log
SET payload = jsonb_strip_nulls(jsonb_build_object(
        'planId', household_plan_id,
        'itemId', budget_item_id,
        'timestamp', occurred_at));
ALTER TABLE audit_log
    ALTER COLUMN payload SET NOT NULL,
    ALTER COLUMN household_plan_id DROP NOT NULL,
    DROP COLUMN budget_item_id;
