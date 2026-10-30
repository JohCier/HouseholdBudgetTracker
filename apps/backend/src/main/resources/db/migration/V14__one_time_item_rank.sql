-- Order of the one-time items within the incomes and the expenses of a year, set by the user
-- (ES-4). Only the order of the ranks counts. Existing items keep their order by date and, on the
-- same date, by entry (ES-6); a new item gets the time it was placed, in microseconds, as rank and so
-- comes last (ES-7).
ALTER TABLE one_time_item ADD COLUMN item_rank NUMERIC;

UPDATE one_time_item item
SET item_rank = ranked.position
FROM (SELECT id,
             row_number() OVER (PARTITION BY owner_id, type, extract(YEAR FROM item_date)
                 ORDER BY item_date, created_at, id) AS position
      FROM one_time_item) ranked
WHERE item.id = ranked.id;

ALTER TABLE one_time_item ALTER COLUMN item_rank SET NOT NULL;
