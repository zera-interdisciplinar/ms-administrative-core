package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterUserTelephoneImpl implements RegisterUserTelephone {
    TelephoneRepository telephoneRepository;
    UserRepository userRepository;
    UnitRepository unitRepository;

    public RegisterUserTelephoneImpl(
            TelephoneRepository telephoneRepository,
            UserRepository userRepository,
            UnitRepository unitRepository) {
        this.telephoneRepository = telephoneRepository;
        this.userRepository = userRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    public RegisterTelephoneOutput execute(RegisterUserTelephoneCommand command) {
        User user = userRepository.findById(command.userId()).orElseThrow( () -> new UserNotFoundException(command.userId()));
        Unit unit = unitRepository.findById(user.getUnitId()).orElseThrow( () -> new UnitNotFoundException(user.getUnitId()));

        TelephoneNumber telephoneNumber = new TelephoneNumber(command.number());
        Telephone telephone = new Telephone(
                UUID.randomUUID(),
                telephoneNumber,
                user.getUserId(),
                unit.getOrganizationId(),
                user.getUnitId()
        );
        telephoneRepository.save(telephone);

        return new RegisterTelephoneOutput(
                telephone.getTelephoneId(),
                telephone.getNumber().value()
        );
    }
}
