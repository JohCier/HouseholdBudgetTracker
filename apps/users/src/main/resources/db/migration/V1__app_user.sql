-- Flyway creates the schema "users" (spring.flyway.schemas) and keeps its history there,
-- separate from the tables of the backend in the same database.
CREATE TABLE users.app_user
(
    id            UUID PRIMARY KEY,
    -- always stored in lower case (see EmailAddress), so a plain unique constraint suffices
    email         VARCHAR(254) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    registered_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT app_user_email_unique UNIQUE (email)
);
