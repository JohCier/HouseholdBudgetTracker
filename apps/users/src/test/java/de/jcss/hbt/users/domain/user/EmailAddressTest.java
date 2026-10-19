package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailAddressTest {

    @Test
    void isNormalizedToLowerCaseWithoutSurroundingWhitespace() {
        assertThat(EmailAddress.of("  Max.Muster@Example.ORG ").value()).isEqualTo("max.muster@example.org");
        assertThat(EmailAddress.of("Max@Example.org")).isEqualTo(EmailAddress.of("max@example.org"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "max", "max@", "@example.org", "max@example", "max muster@example.org",
            "max@@example.org"})
    void rejectsValuesNotShapedLikeAnAddress(String value) {
        assertThatThrownBy(() -> EmailAddress.of(value)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsAddressesLongerThan254Characters() {
        String domain = "@example.org";
        String longest = "a".repeat(EmailAddress.MAX_LENGTH - domain.length()) + domain;

        assertThat(EmailAddress.of(longest).value()).hasSize(EmailAddress.MAX_LENGTH);
        assertThatThrownBy(() -> EmailAddress.of("a" + longest)).isInstanceOf(DomainValidationException.class);
    }
}
