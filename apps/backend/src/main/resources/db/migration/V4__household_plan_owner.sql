-- Household plans now belong to the user who created them (users are managed by the users
-- app, hence no foreign key), and each user may have one plan per year. Existing plans have
-- no owner and are discarded deliberately, their items with them (ON DELETE CASCADE).
DELETE FROM household_plan;

ALTER TABLE household_plan
    ADD COLUMN owner_id UUID NOT NULL;

ALTER TABLE household_plan
    DROP CONSTRAINT household_plan_year_unique;

ALTER TABLE household_plan
    ADD CONSTRAINT household_plan_owner_year_unique UNIQUE (owner_id, year);
