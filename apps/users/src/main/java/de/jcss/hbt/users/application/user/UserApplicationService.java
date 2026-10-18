package de.jcss.hbt.users.application.user;

import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
import de.jcss.hbt.users.domain.exception.LoginLockedException;
import de.jcss.hbt.users.domain.login.LoginThrottle;
import de.jcss.hbt.users.domain.login.LoginThrottleRepository;
import de.jcss.hbt.users.domain.user.EmailAddress;
import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.PasswordHasher;
import de.jcss.hbt.users.domain.user.User;
import de.jcss.hbt.users.domain.user.UserId;
import de.jcss.hbt.users.domain.user.UserRepository;
import de.jcss.hbt.users.domain.user.command.RegisterUserCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Entry point for registering and signing in users. Issuing the session for a signed-in
 * user is up to the web layer; this service only tells who the user is.
 * <p>
 * Contains no business rules of its own. The only check it makes, that the e-mail address
 * is not registered yet, is a process step before registering: it spans several
 * aggregates and gives a clear error early, while the unique constraint behind
 * {@link UserRepository#add} remains the actual safeguard against races.
 * <p>
 * Signing in counts failed attempts per address in a {@link LoginThrottle}, which may lock the
 * address (AN-10).
 */
@Service
@Transactional
public class UserApplicationService {

    private final UserRepository repository;
    private final LoginThrottleRepository throttles;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public UserApplicationService(UserRepository repository, LoginThrottleRepository throttles,
                                  PasswordHasher passwordHasher, ApplicationEventPublisher eventPublisher,
                                  Clock clock) {
        this.repository = Objects.requireNonNull(repository);
        this.throttles = Objects.requireNonNull(throttles);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.clock = Objects.requireNonNull(clock);
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
     * counted, a successful one forgets the failed attempts with the address.
     *
     * @throws LoginLockedException        if the address is locked after too many failed attempts
     * @throws InvalidCredentialsException if there is no such user or the password is wrong
     */
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public UserId authenticate(EmailAddress email, Password password) {
        Instant now = clock.instant();
        LoginThrottle throttle = throttles.findByEmail(email).orElseGet(() -> LoginThrottle.start(email));
        throttle.ensureNotLocked(now);
        try {
            User user = repository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
            user.authenticate(password, passwordHasher);
            throttles.remove(email);
            return user.id();
        } catch (InvalidCredentialsException e) {
            throttles.save(throttle.afterFailedAttempt(now));
            throw e;
        }
    }
}
