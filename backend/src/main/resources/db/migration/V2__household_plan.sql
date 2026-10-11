-- Positions now belong to a household plan per year and no longer have a booking date
-- of their own. Existing positions are discarded deliberately.
DROP TABLE budget_item;

CREATE TABLE household_plan
(
    id         UUID PRIMARY KEY,
    year       INTEGER     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT household_plan_year_unique UNIQUE (year)
);

CREATE TABLE budget_item
(
    id                UUID PRIMARY KEY,
    household_plan_id UUID           NOT NULL REFERENCES household_plan (id) ON DELETE CASCADE,
    description       VARCHAR(255)   NOT NULL,
    amount            NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    type              VARCHAR(16)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category          VARCHAR(32),
    -- clock_timestamp() instead of now() keeps the order of items added in one transaction
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT budget_item_category_matches_type CHECK (
        (type = 'INCOME' AND category IS NULL) OR (type = 'EXPENSE' AND category IS NOT NULL)
    )
);

CREATE INDEX budget_item_household_plan_idx ON budget_item (household_plan_id);

-- Written by event listeners in their own transaction; deliberately without foreign keys,
-- so entries outlive the plans and items they refer to.
CREATE TABLE audit_log
(
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type        VARCHAR(64) NOT NULL,
    household_plan_id UUID        NOT NULL,
    budget_item_id    UUID,
    recorded_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
