package de.jcss.hbt.users.infrastructure.security;

import de.jcss.hbt.users.domain.user.Password;
import de.jcss.hbt.users.domain.user.PasswordHash;
import de.jcss.hbt.users.domain.user.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Hashes passwords with BCrypt (salted, deliberately slow). */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(Password password) {
        return PasswordHash.of(encoder.encode(password.value()));
    }

    @Override
    public boolean matches(Password password, PasswordHash hash) {
        return encoder.matches(password.value(), hash.value());
    }
}
