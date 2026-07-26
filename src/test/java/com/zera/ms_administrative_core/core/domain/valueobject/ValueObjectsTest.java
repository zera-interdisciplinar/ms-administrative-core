package com.zera.ms_administrative_core.core.domain.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCnpjException;
import org.junit.jupiter.api.Test;

class ValueObjectsTest {

    @Test
    void emailShouldNormalizeValidateAndExposeDomain() {
        Email email = new Email("  USER@Example.com  ");

        assertEquals("user@example.com", email.email());
        assertEquals("example.com", email.domain());
        assertEquals("user@example.com", email.value());
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

    @Test
    void cnpjShouldNormalizeAndValidate() {
        Cnpj cnpj = new Cnpj("11.222.333/0001-81");

        assertEquals("11222333000181", cnpj.value());
    }

    @Test
    void cnpjShouldRejectInvalidInput() {
        InvalidCnpjException invalidCnpj = assertThrows(InvalidCnpjException.class,
                () -> new Cnpj("11.222.333/0001-00"));
        InvalidCnpjException repeatedDigits = assertThrows(InvalidCnpjException.class,
                () -> new Cnpj("11111111111111"));
        NullPointerException nullCnpj = assertThrows(NullPointerException.class,
                () -> new Cnpj(null));

        assertEquals("Invalid CNPJ: 11.222.333/0001-00", invalidCnpj.getMessage());
        assertEquals("Invalid CNPJ: 11111111111111", repeatedDigits.getMessage());
        assertEquals("CNPJ cannot be null", nullCnpj.getMessage());
    }
}
