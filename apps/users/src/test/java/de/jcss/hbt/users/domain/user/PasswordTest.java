package de.jcss.hbt.users.domain.user;

import de.jcss.hbt.users.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    @Test
    void needsAtLeastEightCharacters() {
        assertThat(Password.of("12345678").value()).isEqualTo("12345678");
        assertThatThrownBy(() -> Password.of("1234567")).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Password.of(null)).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void countsCharactersNotBytesForTheMinimum() {
        assertThat(Password.of("äöüßäöüß").value()).hasSize(8);
    }

    @Test
    void mayNotExceedWhatBCryptTakesIntoAccount() {
        assertThat(Password.of("a".repeat(72)).value()).hasSize(72);
        assertThatThrownBy(() -> Password.of("a".repeat(73))).isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> Password.of("ä".repeat(37))).as("74 bytes in UTF-8")
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void doesNotRevealItselfInToString() {
        assertThat(Password.of("geheim123")).hasToString("Password[***]");
        assertThat(PasswordHash.of("$2a$10$abc")).hasToString("PasswordHash[***]");
    }
}
