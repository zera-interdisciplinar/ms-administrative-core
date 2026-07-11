package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

public class Manager extends User {
      public Manager(UUID userId, String name, Email email,
                  HashedPassword password, Status status, UUID unitId,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
            super(userId, name, email, password, status, unitId, createdAt, updatedAt);
      }

      @Override
      public Role role(){
            return Role.MANAGER;
      }
}
