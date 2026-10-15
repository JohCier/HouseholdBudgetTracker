package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.exception.InvalidEmailAddressException;
import de.jcss.hbt.users.domain.shared.ValueObject;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The e-mail address a user signs in with. Normalized to lower case without surrounding
 * whitespace, so that {@code Max@Example.org} and {@code max@example.org} are the same
 * address and thus the same account. Only the rough shape is checked; whether the
 * address actually exists is not.
 */
@ValueObject
public record EmailAddress(String value) {

    /** Maximum length of an address according to RFC 5321. */
    public static final int MAX_LENGTH = 254;

    private static final Pattern SHAPE = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    public EmailAddress {
        if (value == null) {
            throw new InvalidEmailAddressException("e-mail address must not be null");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new InvalidEmailAddressException("e-mail address must not be longer than %d characters"
                    .formatted(MAX_LENGTH));
        }
        if (!SHAPE.matcher(value).matches()) {
            throw new InvalidEmailAddressException("'%s' is not a valid e-mail address".formatted(value));
        }
    }

    /**
     * @throws InvalidEmailAddressException if the value is missing or not shaped like an e-mail address
     */
    public static EmailAddress of(String value) {
        return new EmailAddress(value);
    }
}
