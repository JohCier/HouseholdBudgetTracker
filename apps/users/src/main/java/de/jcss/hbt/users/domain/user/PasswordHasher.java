package de.jcss.hbt.users.domain.user;

/**
 * Turns passwords into {@link PasswordHash}es and checks passwords against them. Declared
 * here so that a {@link User} can protect its password without knowing the algorithm;
 * implemented in the infrastructure. Implementations must use a slow, salted hash.
 */
public interface PasswordHasher {

    PasswordHash hash(Password password);

    boolean matches(Password password, PasswordHash hash);
}
