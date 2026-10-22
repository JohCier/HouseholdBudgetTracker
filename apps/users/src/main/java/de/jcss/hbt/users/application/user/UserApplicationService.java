package de.jcss.hbt.users.application.user;

import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.PasswordHash;
import de.jcss.hbt.users.domain.user.PasswordHasher;
import de.jcss.hbt.users.domain.user.User;
import de.jcss.hbt.users.domain.user.UserId;
import de.jcss.hbt.users.domain.user.UserRepository;
import de.jcss.hbt.users.domain.exception.PasswordUnchangedException;
import de.jcss.hbt.users.domain.exception.WrongCurrentPasswordException;
import de.jcss.hbt.users.domain.user.command.ChangePasswordCommand;
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entry point for registering and signing in users and for changing passwords. Issuing the session for a signed-in
 * user is up to the web layer; this service only tells who the user is.
 * <p>
 * Contains no business rules of its own. The only check it makes, that the e-mail address
 * is not registered yet, is a process step before registering: it spans several
 * aggregates and gives a clear error early, while the unique constraint behind
 * {@link UserRepository#add} remains the actual safeguard against races.
 * <p>
 * Runs without a database transaction on purpose: hashing and checking passwords is
 * deliberately slow (BCrypt), and a transaction would hold one of the few pooled database
 * connections all that time. None is needed either, since each use case writes at most one
 * row: registering stores the user with a single {@code INSERT}, signing in only reads.
 * <p>
 * Signing in counts every attempt with the {@link LoginThrottle} before the password is
 * checked, which may lock the address (AN-10); a successful attempt resets the count. Changing
 * the password counts the same way, so it cannot be used to try out passwords (PA-6).
 */
@Service
public class UserApplicationService {

    private final UserRepository repository;
    private final LoginThrottle loginThrottle;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;
    /** Checked against when nobody has the address, see {@link #authenticate}. */
    private final PasswordHash unknownUserHash;

    public UserApplicationService(UserRepository repository, LoginThrottle loginThrottle,
                                  PasswordHasher passwordHasher, ApplicationEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.loginThrottle = Objects.requireNonNull(loginThrottle);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.unknownUserHash = passwordHasher.hash(Password.of(UUID.randomUUID().toString()));
    }

    /**
     * @throws EmailAddressAlreadyRegisteredException if there already is a user with the address
     */
    public UserId register(RegisterUserCommand command) {
        if (repository.existsByEmail(command.email())) {
            throw new EmailAddressAlreadyRegisteredException(command.email());
        }
        User user = User.register(UserId.generate(), command.email(), command.password(), passwordHasher);
        repository.add(user);
        user.pullDomainEvents().forEach(eventPublisher::publishEvent);
        return user.id();
    }

    /**
     * Finds out who signs in with the given e-mail address and password. The attempt is counted
     * as failed up front, so parallel attempts cannot get past the lock; a successful one forgets
     * the failed attempts with the address. For an address nobody registered, the password is
     * still checked (against a hash no password matches), so the answer takes as long as for a
     * wrong password and does not tell which addresses exist (AN-6).
     *
     * @throws LoginLockedException        if the address is locked after too many failed attempts
     * @throws InvalidCredentialsException if there is no such user or the password is wrong
     */
    public UserId authenticate(EmailAddress email, Password password) {
        loginThrottle.countAttempt(email);
        Optional<User> user = repository.findByEmail(email);
        if (user.isEmpty()) {
            passwordHasher.matches(password, unknownUserHash);
            throw new InvalidCredentialsException();
        }
        user.get().authenticate(password, passwordHasher);
        loginThrottle.reset(email);
        return user.get().id();
    }

    /**
     * Changes the password of the signed-in user. Like signing in, the attempt is counted up front
     * with the user's address and forgotten once the password is changed (PA-6).
     *
     * @throws LoginLockedException          if the user's address is locked after too many failed attempts
     * @throws WrongCurrentPasswordException if the current password is wrong
     * @throws PasswordUnchangedException    if the new password is the current one
     * @throws InvalidCredentialsException   if the session belongs to no user
     */
    public void changePassword(ChangePasswordCommand command) {
        User user = repository.findById(command.userId()).orElseThrow(InvalidCredentialsException::new);
        loginThrottle.countAttempt(user.email());
        User changed = user.changePassword(command.currentPassword(), command.newPassword(), passwordHasher);
        loginThrottle.reset(user.email());
        repository.save(changed);
        changed.pullDomainEvents().forEach(eventPublisher::publishEvent);
    }
}
