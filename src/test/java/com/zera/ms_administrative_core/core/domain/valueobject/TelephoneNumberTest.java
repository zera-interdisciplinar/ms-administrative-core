package com.zera.ms_administrative_core.core.domain.valueobject;

import com.zera.ms_administrative_core.core.domain.exception.InvalidTelephoneNumberException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TelephoneNumberTest {

    @Test
    @DisplayName("Should normalize a formatted mobile number to digits only")
    void shouldNormalizeFormattedNumber() {
        TelephoneNumber number = new TelephoneNumber("(11) 98765-4321");

        assertEquals("11987654321", number.value());
    }

    @Test
    @DisplayName("Should accept a 10-digit landline number")
    void shouldAcceptLandline() {
        assertEquals("1133334444", new TelephoneNumber("1133334444").value());
    }

    @Test
    @DisplayName("Should format an 11-digit mobile number")
    void shouldFormatMobile() {
        assertEquals("(11) 98765-4321", new TelephoneNumber("11987654321").formatted());
    }

    @Test
    @DisplayName("Should format a 10-digit landline number")
    void shouldFormatLandline() {
        assertEquals("(11) 3333-4444", new TelephoneNumber("1133334444").formatted());
    }

    @Test
    @DisplayName("Should reject a number with an invalid length")
    void shouldRejectInvalidLength() {
        InvalidTelephoneNumberException ex = assertThrows(InvalidTelephoneNumberException.class,
                () -> new TelephoneNumber("123"));
        assertEquals("Invalid telephone number: 123", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject a null number")
    void shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> new TelephoneNumber(null));
    }
}
