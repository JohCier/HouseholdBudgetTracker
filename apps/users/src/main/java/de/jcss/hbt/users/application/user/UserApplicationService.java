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
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Entry point for registering and signing in users. Issuing the session for a signed-in
 * user is up to the web layer; this service only tells who the user is.
 * <p>
 * Contains no business rules of its own. The only check it makes, that the e-mail address
 * is not registered yet, is a process step before registering: it spans several
 * aggregates and gives a clear error early, while the unique constraint behind
 * {@link UserRepository#add} remains the actual safeguard against races.
 * <p>
 * Signing in reports failed attempts to the {@link LoginThrottle}, which may lock the address
 * (AN-10).
 */
@Service
@Transactional
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
     * Finds out who signs in with the given e-mail address and password. A failed attempt is
     * counted, a successful one forgets the failed attempts with the address. For an address
     * nobody registered, the password is still checked (against a hash no password matches),
     * so the answer takes as long as for a wrong password and does not tell which addresses
     * exist (AN-6).
     *
     * @throws LoginLockedException        if the address is locked after too many failed attempts
     * @throws InvalidCredentialsException if there is no such user or the password is wrong
     */
    public UserId authenticate(EmailAddress email, Password password) {
        loginThrottle.ensureNotLocked(email);
        try {
            Optional<User> user = repository.findByEmail(email);
            if (user.isEmpty()) {
                passwordHasher.matches(password, unknownUserHash);
                throw new InvalidCredentialsException();
            }
            user.get().authenticate(password, passwordHasher);
            loginThrottle.reset(email);
            return user.get().id();
        } catch (InvalidCredentialsException e) {
            loginThrottle.recordFailure(email);
            throw e;
        }
    }
}
