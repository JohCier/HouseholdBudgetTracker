-- One-time incomes and expenses (EP-1 to EP-5): they belong to their owner directly, not to a
-- household plan, so deleting a plan leaves them untouched (EP-5). The year they count for is
-- the year of their date (EP-3).
CREATE TABLE one_time_item
(
    id          UUID PRIMARY KEY,
    owner_id    UUID           NOT NULL,
    type        VARCHAR(16)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    description VARCHAR(255)   NOT NULL,
    amount      NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    item_date   DATE           NOT NULL,
    note        VARCHAR(1000),
    -- Items on the same date are shown in the order they were entered (EP-8)
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX one_time_item_owner_date_idx ON one_time_item (owner_id, item_date);
