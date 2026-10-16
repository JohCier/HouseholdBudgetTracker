package de.jcss.hbt.users.application.user;

import de.jcss.hbt.users.domain.exception.EmailAddressAlreadyRegisteredException;
import de.jcss.hbt.users.domain.exception.InvalidCredentialsException;
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

import java.util.Objects;

/**
 * Entry point for registering and signing in users. Issuing the session for a signed-in
 * user is up to the web layer; this service only tells who the user is.
 * <p>
 * Contains no business rules of its own. The only check it makes, that the e-mail address
 * is not registered yet, is a process step before registering: it spans several
 * aggregates and gives a clear error early, while the unique constraint behind
 * {@link UserRepository#add} remains the actual safeguard against races.
 */
@Service
@Transactional
public class UserApplicationService {

    private final UserRepository repository;
    private final PasswordHasher passwordHasher;
    private final ApplicationEventPublisher eventPublisher;

    public UserApplicationService(UserRepository repository, PasswordHasher passwordHasher,
                                  ApplicationEventPublisher eventPublisher) {
        this.repository = Objects.requireNonNull(repository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
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
     * Finds out who signs in with the given e-mail address and password.
     *
     * @throws InvalidCredentialsException if there is no such user or the password is wrong
     */
    @Transactional(readOnly = true)
    public UserId authenticate(EmailAddress email, Password password) {
        User user = repository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        user.authenticate(password, passwordHasher);
        return user.id();
    }
}
