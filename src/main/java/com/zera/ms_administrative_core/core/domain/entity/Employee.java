package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

public class Employee extends User {
      private UUID managerId;

      public Employee(UUID userId, String name, Email email,
                  HashedPassword password, Status status, UUID unitId,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
            this(userId, name, email, password, status, unitId, createdAt, updatedAt, null);
      }

      public Employee(UUID userId, String name, Email email,
                  HashedPassword password, Status status, UUID unitId,
                  LocalDateTime createdAt, LocalDateTime updatedAt, UUID managerId) {
            super(userId, name, email, password, status, unitId, createdAt, updatedAt);
            this.managerId = managerId;
      }

      public UUID getManagerId() {
            return managerId;
      }

      public void assignManagerId(UUID managerId) {
            this.managerId = managerId;
      }

      @Override
      public Role role(){
            return Role.EMPLOYEE;
      }
}
