package com.zera.ms_administrative_core.core.usecase.user.findUser;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class FindUserByEmailImpl implements FindUserByEmail {

    private final UserRepository repository;

    public FindUserByEmailImpl(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserOutput execute(String email) {
        User user = repository.findByEmail(new Email(email))
                .orElseThrow(() -> new UserNotFoundException(new Email(email)));
        return UserOutput.from(user);
    }
}