-- Whether the stored amount is a monthly or a yearly amount (MJ-1, MJ-2); existing items are yearly (MJ-3).
ALTER TABLE budget_item
    ADD COLUMN amount_period VARCHAR(10) NOT NULL DEFAULT 'YEARLY'
        CHECK (amount_period IN ('MONTHLY', 'YEARLY'));
