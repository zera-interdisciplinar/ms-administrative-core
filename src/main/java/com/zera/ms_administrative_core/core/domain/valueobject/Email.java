package com.zera.ms_administrative_core.core.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;
import java.util.regex.Pattern;

public record Email(String email) {
      private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
      
      public Email {
            Objects.requireNonNull(email, "Email cannot be null");
            String normalized = email.trim().toLowerCase();

            if (!EMAIL_PATTERN.matcher(normalized).matches()) {
                  throw new IllegalArgumentException("Invalid email: " + email);
              }
              email = normalized;
      }

      public String domain() {
            return email.substring(email.indexOf('@') + 1);
      }

      @JsonValue
      public String value() {
            return email;
      }
}
