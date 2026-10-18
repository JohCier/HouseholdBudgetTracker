-- Optional free text per position (PN-1); at most 1000 characters (PN-2), NULL means no note.
ALTER TABLE budget_item
    ADD COLUMN note VARCHAR(1000);
