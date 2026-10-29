-- Deleting a plan only moves it to the deleted plans (GP-7): it keeps its items and remembers
-- when it was deleted (GP-2). A user has at most one active plan per year, but any number of
-- deleted ones (GP-1).
ALTER TABLE household_plan
    ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE household_plan
    DROP CONSTRAINT household_plan_owner_year_unique;

CREATE UNIQUE INDEX household_plan_owner_year_active_unique
    ON household_plan (owner_id, year)
    WHERE deleted_at IS NULL;
