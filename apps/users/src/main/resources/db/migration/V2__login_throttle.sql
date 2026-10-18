-- Failed sign-in attempts per e-mail address (AN-10, see LoginThrottle). A row exists from the
-- first failed attempt until the next successful sign-in; the address need not be registered.
CREATE TABLE users.login_throttle
(
    email             VARCHAR(254) PRIMARY KEY,
    failed_attempts   INTEGER      NOT NULL,
    window_started_at TIMESTAMPTZ,
    locked_until      TIMESTAMPTZ
);
