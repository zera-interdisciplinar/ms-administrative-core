package com.zera.ms_administrative_core.core.domain.valueobject;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCepException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CepTest {

    @Test
    @DisplayName("Should normalize a formatted CEP to digits only")
    void shouldNormalizeFormattedCep() {
        Cep cep = new Cep("01001-000");

        assertEquals("01001000", cep.value());
    }

    @Test
    @DisplayName("Should accept a CEP that already contains only digits")
    void shouldAcceptDigitsOnly() {
        assertEquals("12345678", new Cep("12345678").value());
    }

    @Test
    @DisplayName("Should format the CEP back with a dash")
    void shouldFormatWithDash() {
        assertEquals("01001-000", new Cep("01001000").formatted());
    }

    @Test
    @DisplayName("Should reject a CEP with the wrong number of digits")
    void shouldRejectInvalidLength() {
        InvalidCepException ex = assertThrows(InvalidCepException.class, () -> new Cep("123"));
        assertEquals("invalid cep: 123", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject a null CEP")
    void shouldRejectNull() {
        assertThrows(NullPointerException.class, () -> new Cep(null));
    }
}
