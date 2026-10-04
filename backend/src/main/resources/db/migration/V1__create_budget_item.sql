CREATE TABLE budget_item
(
    id           UUID PRIMARY KEY,
    description  VARCHAR(255)   NOT NULL,
    amount       NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    type         VARCHAR(16)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    category     VARCHAR(32),
    booking_date DATE           NOT NULL,
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT budget_item_category_matches_type CHECK (
        (type = 'INCOME' AND category IS NULL) OR (type = 'EXPENSE' AND category IS NOT NULL)
    )
);

CREATE INDEX budget_item_booking_date_idx ON budget_item (booking_date);
