-- Optional subcategory per position (UK-1); at most 50 characters (UK-2), NULL means none ("Sonstiges").
ALTER TABLE budget_item
    ADD COLUMN subcategory VARCHAR(50);

-- Display order of the positions of a plan (UK-14): the highest position is shown on top. Existing
-- positions keep their order so far, the most recently added on top (UK-16).
ALTER TABLE budget_item
    ADD COLUMN position INTEGER;

UPDATE budget_item
SET position = ranked.position
FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY household_plan_id ORDER BY created_at, id) - 1 AS position
      FROM budget_item) AS ranked
WHERE budget_item.id = ranked.id;

ALTER TABLE budget_item
    ALTER COLUMN position SET NOT NULL;

-- The order the user arranged the subcategory groups of a section in (UK-14); a section is either
-- INCOME or the category of the expenses. Sections never arranged have no rows.
CREATE TABLE subcategory_order
(
    household_plan_id UUID        NOT NULL REFERENCES household_plan (id) ON DELETE CASCADE,
    section           VARCHAR(32) NOT NULL,
    position          INTEGER     NOT NULL,
    subcategory       VARCHAR(50) NOT NULL,
    PRIMARY KEY (household_plan_id, section, position)
);
