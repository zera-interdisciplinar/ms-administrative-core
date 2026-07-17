package com.zera.ms_administrative_core.core.usecase.findUser;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FindUserByIdImpl implements FindUserById {

    private final UserRepository repository;

    public FindUserByIdImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserOutput execute(UUID id) {
        User user = repository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return UserOutput.from(user);
    }
}