package com.zera.ms_administrative_core.core.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID id) {
      public UserId {
            Objects.requireNonNull(id, "UserId cannot be null");
      }

      public static UserId newUserId(){
            return new UserId(UUID.randomUUID());
      }

      // returns a valid UserId from a String
      public static UserId fromString(String id) {
            return new UserId(UUID.fromString(id));
      }

      @Override
      public String toString() {
            return id.toString();
      }
}
