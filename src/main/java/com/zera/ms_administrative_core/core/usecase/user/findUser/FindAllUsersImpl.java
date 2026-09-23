package com.zera.ms_administrative_core.core.usecase.user.findUser;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class FindAllUsersImpl implements FindAllUsers {

    private final UserRepository repository;

    public FindAllUsersImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UserOutput> execute(Role role, Status status, UUID managerId, int page, int size) {
        return repository.findAll(role, status, managerId, page, size).stream()
                .map(UserOutput::from)
                .toList();
    }
}