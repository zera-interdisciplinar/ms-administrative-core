package com.zera.ms_administrative_core.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.zera.ms_administrative_core.core.domain.exception.InvalidTelephoneNumberException;

import java.util.Objects;

public record TelephoneNumber(String value) {

    public TelephoneNumber {
        Objects.requireNonNull(value, "Telephone number cannot be null");
        String normalized = value.replaceAll("\\D", "");

        if (!isValid(normalized)) {
            throw new InvalidTelephoneNumberException(value);
        }

        value = normalized;
    }

    private static boolean isValid(String digits) {
        return digits.length() == 10 || digits.length() == 11;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public String formatted() {
        String ddd = value.substring(0, 2);
        String rest = value.substring(2);
        String prefix = rest.substring(0, rest.length() - 4);
        String suffix = rest.substring(rest.length() - 4);
        return "(" + ddd + ") " + prefix + "-" + suffix;
    }
}
