-- When the logged change happened in the domain, as opposed to when it was written to the log.
ALTER TABLE audit_log
    ADD COLUMN occurred_at TIMESTAMPTZ;
UPDATE audit_log SET occurred_at = recorded_at;
ALTER TABLE audit_log
    ALTER COLUMN occurred_at SET NOT NULL;
