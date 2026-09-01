package com.zera.ms_administrative_core.core.domain.valueobject;

import com.zera.ms_administrative_core.core.domain.exception.InvalidCepException;

import java.util.Objects;

public record Cep(String value) {
    public Cep {
        Objects.requireNonNull(value,"CEP cannot be null");
        String normalized = value.replaceAll("\\D", "");

        if(!isValid(normalized)) {
            throw new InvalidCepException(value);
        }

        value = normalized;
    }
    private static boolean isValid(String digits) {
        return digits.length() == 8;
    }

    public String value(){
        return value;
    }

    public String formatted() {
        return value.substring(0, 5) + "-" + value.substring(5);
    }
}
