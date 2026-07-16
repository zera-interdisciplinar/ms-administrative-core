package com.zera.ms_administrative_core.core.domain.valueobject;

import java.util.Objects;

public record HashedPassword(String hash) {
      public HashedPassword {
            Objects.requireNonNull(hash, "Hash cannot be null");
      }

      @Override
      public String toString() {
            return "PROTECTED";
      }
}