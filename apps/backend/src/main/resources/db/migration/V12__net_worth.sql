-- Assets and liabilities of a user at the end of a year (VS-1, VS-2), independent of the household plan
-- of that year (VS-4). Users are managed by the users app, hence no foreign key; one statement per user
-- and year, which exists as long as it has entries (VS-7).
CREATE TABLE net_worth_statement
(
    id         UUID PRIMARY KEY,
    owner_id   UUID        NOT NULL,
    year       INTEGER     NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT net_worth_statement_owner_year_unique UNIQUE (owner_id, year)
);

-- Liabilities have a positive amount as well (VS-1); position keeps the order they were recorded in (VS-8).
CREATE TABLE net_worth_entry
(
    id                     UUID PRIMARY KEY,
    net_worth_statement_id UUID           NOT NULL REFERENCES net_worth_statement (id) ON DELETE CASCADE,
    type                   VARCHAR(16)    NOT NULL CHECK (type IN ('ASSET', 'LIABILITY')),
    name                   VARCHAR(255)   NOT NULL,
    amount                 NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    position               INTEGER        NOT NULL
);

CREATE INDEX net_worth_entry_statement_idx ON net_worth_entry (net_worth_statement_id);
