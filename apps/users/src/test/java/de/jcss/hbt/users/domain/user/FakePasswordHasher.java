package de.jcss.hbt.users.domain.user;

/** Reversible stand-in for a real hasher, so tests run fast and can see what was hashed. */
public final class FakePasswordHasher implements PasswordHasher {

    @Override
    public PasswordHash hash(Password password) {
        return PasswordHash.of("hashed:" + password.value());
    }

    @Override
    public boolean matches(Password password, PasswordHash hash) {
        return hash.equals(hash(password));
    }
}
