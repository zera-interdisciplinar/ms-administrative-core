package com.zera.ms_administrative_core.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCnpjException;

import java.util.Objects;

public record Cnpj(String value) {

    private static final int[] FIRST_CHECK_WEIGHTS = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] SECOND_CHECK_WEIGHTS = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    public Cnpj {
        Objects.requireNonNull(value, "CNPJ cannot be null");
        String normalized = value.replaceAll("\\D", "");

        if (!isValid(normalized)) {
            throw new InvalidCnpjException(value);
        }

        value = normalized;
    }

    private static boolean isValid(String digits) {
        if (digits.length() != 14 || digits.chars().allMatch(c -> c == digits.charAt(0))) {
            return false;
        }

        int firstCheckDigit = calculateCheckDigit(digits, FIRST_CHECK_WEIGHTS);
        int secondCheckDigit = calculateCheckDigit(digits, SECOND_CHECK_WEIGHTS);

        return firstCheckDigit == Character.getNumericValue(digits.charAt(12))
                && secondCheckDigit == Character.getNumericValue(digits.charAt(13));
    }

    private static int calculateCheckDigit(String digits, int[] weights) {
        int sum = 0;

        for (int i = 0; i < weights.length; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weights[i];
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
