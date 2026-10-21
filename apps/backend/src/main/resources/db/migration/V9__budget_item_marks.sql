-- Marks of a position (PM-1, PM-2): whether it is important, and the monthly saving potential of an
-- expense (PM-2a); NULL means none. Existing items are neither important nor have a saving potential.
ALTER TABLE budget_item
    ADD COLUMN important        BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN saving_potential NUMERIC(12, 2) CHECK (saving_potential > 0),
    ADD CONSTRAINT budget_item_saving_potential_only_for_expenses
        CHECK (saving_potential IS NULL OR type = 'EXPENSE');
