package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.exception.PasswordUnchangedException;
import de.jcss.hbt.users.domain.exception.WrongCurrentPasswordException;
import de.jcss.hbt.users.domain.user.event.PasswordChanged;
import de.jcss.hbt.users.domain.user.event.UserRegistered;
import de.jcss.hbt.users.domain.user.valueobject.EmailAddress;
import de.jcss.hbt.users.domain.user.valueobject.Password;
import de.jcss.hbt.users.domain.user.valueobject.PasswordHash;
import de.jcss.hbt.users.domain.user.valueobject.UserId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.jmolecules.event.types.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Aggregate root for a person who can sign in, identified by their {@link EmailAddress}
 * and a password. The password itself is never kept, only its {@link PasswordHash};
 * whether a password is right is decided by the user itself ({@link #authenticate}).
 * Changing the password ({@link #changePassword}) yields the user with the new hash, since
 * all fields are final.
 * <p>
 * That an e-mail address belongs to at most one user spans several aggregates and is
 * therefore ensured by the application layer and the repository, not by this class.
 * <p>
 * Identity (equals/hashCode) is based solely on the user id.
 */
@AggregateRoot
public final class User {

    @Identity
    private final UserId id;
    private final EmailAddress email;
    private final PasswordHash passwordHash;
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private User(UserId id, EmailAddress email, PasswordHash passwordHash) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = Objects.requireNonNull(email, "email must not be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash must not be null");
    }

    /** Registers a new user who will sign in with the given e-mail address and password. */
    public static User register(UserId id, EmailAddress email, Password password, PasswordHasher hasher) {
        Objects.requireNonNull(password, "password must not be null");
        User user = new User(id, email, hasher.hash(password));
        user.domainEvents.add(new UserRegistered(id, Instant.now()));
        return user;
    }

    /** Rebuilds a stored user, e.g. in a repository; records no events. */
    public static User reconstitute(UserId id, EmailAddress email, PasswordHash passwordHash) {
        return new User(id, email, passwordHash);
    }

    /**
     * Checks that the password is this user's.
     *
     * @throws InvalidCredentialsException if it is not
     */
    public void authenticate(Password password, PasswordHasher hasher) {
        if (!hasher.matches(password, passwordHash)) {
            throw new InvalidCredentialsException();
        }
    }

    /**
     * Changes the password, provided the current one is right (PA-2) and the new one differs
     * from it (PA-4). Returns this user with the new password, which is to be saved; pending
     * events are carried over.
     *
     * @throws WrongCurrentPasswordException if the current password is not this user's
     * @throws PasswordUnchangedException    if the new password is the current one
     */
    public User changePassword(Password currentPassword, Password newPassword, PasswordHasher hasher) {
        Objects.requireNonNull(newPassword, "newPassword must not be null");
        if (!hasher.matches(currentPassword, passwordHash)) {
            throw new WrongCurrentPasswordException();
        }
        if (newPassword.equals(currentPassword)) {
            throw new PasswordUnchangedException();
        }
        User changed = new User(id, email, hasher.hash(newPassword));
        changed.domainEvents.addAll(domainEvents);
        changed.domainEvents.add(new PasswordChanged(id, Instant.now()));
        return changed;
    }

    /**
     * Returns the events recorded since the last call and forgets them, so that each
     * event is published exactly once.
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }

    public UserId id() {
        return id;
    }

    public EmailAddress email() {
        return email;
    }

    /** For storing the user; checking a password is up to {@link #authenticate}. */
    public PasswordHash passwordHash() {
        return passwordHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "User{id=%s, email=%s}".formatted(id.value(), email.value());
    }
}
