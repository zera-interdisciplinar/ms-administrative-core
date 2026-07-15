package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

public abstract class User {
      private final UUID userId;
      private String name;

      // password must be encrypted in infra layer
      private HashedPassword password;

      private Email email;
      private Status status;

      // reference to unit that the user belongs to
      private final UUID unitId;

      private final LocalDateTime createdAt;
      private LocalDateTime updatedAt;

      protected User(UUID userId, String name, Email email,
                  HashedPassword password, Status status, UUID unitId) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.password = password;
            this.status = status;
            this.unitId = unitId;
            this.createdAt = LocalDateTime.now();
            this.updatedAt = LocalDateTime.now();
      }

      // used when loading data from database
      protected User(UUID userId, String name, Email email,
                  HashedPassword password, Status status, UUID unitId,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.password = password;
            this.status = status;
            this.unitId = unitId;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
      }

      public abstract Role role();

      // GETTERS
      public UUID getUserId() {
            return userId;
      }

      public String getName() {
            return name;
      }

      public Email getEmail() {
            return email;
      }

      public HashedPassword getPassword() {
            return password;
      }

      public Status getStatus() {
            return status;
      }

      public UUID getUnitId() {
            return unitId;
      }

      public LocalDateTime getCreatedAt() {
            return createdAt;
      }

      public LocalDateTime getUpdatedAt() {
            return updatedAt;
      }

      // ------------------------------------------

      private void touch() {
            this.updatedAt = LocalDateTime.now();
      }

      public void rename(String newName) {
            this.name = newName;
            touch();
      }

      public void changeEmail(Email newEmail) {
            this.email = newEmail;
            touch();
      }

      public void activate() {
            this.status = Status.ACTIVE;
            touch();
      }

      public void deactivate() {
            this.status = Status.INACTIVE;
            touch();
      }

      public void suspend() {
            this.status = Status.SUSPENDED;
            touch();
      }

      public void changePassword(HashedPassword newPassword) {
            this.password = newPassword;
            touch();
      }
      // TODO: think about if we should have a method to change the unit of the user
}
