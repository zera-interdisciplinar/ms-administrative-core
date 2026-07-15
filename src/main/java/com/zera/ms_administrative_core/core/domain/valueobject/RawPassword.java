package com.zera.ms_administrative_core.core.domain.valueobject;

import java.util.Objects;

public record RawPassword(String value) {
      public RawPassword {
            Objects.requireNonNull(value, "Raw password cannot be null");
      }

      @Override
      public String toString() {
            return "PROTECTED";
      }
}