package com.zera.ms_administrative_core.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

public interface UserRepository {
      User insert(User user);
      User delete(User user);
      User update(User user);
      Optional<User> findById(UUID id);
      Optional<User> findByEmail(Email email);
      List<User> findAll();
}