package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;

import java.util.Optional;

/** Collection of all {@link User} aggregates; always loads and stores a user as a whole. */
public interface UserRepository {

    /** The user who signs in with the given address, if there is one. */
    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);

    /**
     * Adds a newly registered user to the collection.
     *
     * @throws EmailAddressAlreadyRegisteredException if there already is a user with the same address
     */
    void add(User user);
}
