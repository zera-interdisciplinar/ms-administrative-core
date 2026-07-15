package com.zera.ms_administrative_core.core.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    @Test
    void emailShouldNormalizeValidateAndExposeDomain() {
        Email email = new Email("  USER@Example.com  ");

        assertEquals("user@example.com", email.email());
        assertEquals("example.com", email.domain());
        assertEquals("user@example.com", email.toString());
    }

    @Test
    void emailShouldRejectInvalidInput() {
        IllegalArgumentException invalidEmail = assertThrows(IllegalArgumentException.class,
                () -> new Email("invalid-email"));
        NullPointerException nullEmail = assertThrows(NullPointerException.class,
                () -> new Email(null));

        assertEquals("Invalid email: invalid-email", invalidEmail.getMessage());
        assertEquals("Email cannot be null", nullEmail.getMessage());
    }

    @Test
    void rawPasswordShouldProtectStringRepresentation() {
        RawPassword rawPassword = new RawPassword("secret");

        assertEquals("secret", rawPassword.value());
        assertEquals("PROTECTED", rawPassword.toString());
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new RawPassword(null));
        assertEquals("Raw password cannot be null", exception.getMessage());
    }

    @Test
    void hashedPasswordShouldProtectStringRepresentation() {
        HashedPassword hashedPassword = new HashedPassword("hash");

        assertEquals("hash", hashedPassword.hash());
        assertEquals("PROTECTED", hashedPassword.toString());
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new HashedPassword(null));
        assertEquals("Hash cannot be null", exception.getMessage());
    }
}
