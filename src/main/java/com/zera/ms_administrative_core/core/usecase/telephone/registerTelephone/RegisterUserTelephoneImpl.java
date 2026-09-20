package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterUserTelephoneImpl implements RegisterUserTelephone {
    private final TelephoneRepository telephoneRepository;
    private final UserRepository userRepository;

    public RegisterUserTelephoneImpl(
            TelephoneRepository telephoneRepository,
            UserRepository userRepository) {
        this.telephoneRepository = telephoneRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RegisterTelephoneOutput execute(RegisterUserTelephoneCommand command) {
        User user = userRepository.findById(command.userId()).orElseThrow( () -> new UserNotFoundException(command.userId()));

        if (telephoneRepository.findByUserId(user.getUserId()).isPresent()) {
            throw TelephoneAlreadyRegisteredException.forUser(user.getUserId());
        }

        TelephoneNumber telephoneNumber = new TelephoneNumber(command.number());
        Telephone telephone = Telephone.forUser(
                UUID.randomUUID(),
                telephoneNumber,
                user.getUserId()
        );
        telephoneRepository.save(telephone);

        return new RegisterTelephoneOutput(
                telephone.getTelephoneId(),
                telephone.getNumber().value()
        );
    }
}
