package com.zera.ms_administrative_core.core.usecase.findUser;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindAllUsersImpl implements FindAllUsers {

    private final UserRepository repository;

    public FindAllUsersImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<UserOutput> execute(Role role, Status status, int size, int page) {
        return repository.findAll(role, status, size, page).stream()
                .map(UserOutput::from)
                .toList();
    }
}