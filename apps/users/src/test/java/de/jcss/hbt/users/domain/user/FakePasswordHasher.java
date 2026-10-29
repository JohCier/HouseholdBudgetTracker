package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.user.valueobject.Password;
import de.jcss.hbt.users.domain.user.valueobject.PasswordHash;

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
