package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterUnitTelephoneImpl implements RegisterUnitTelephone {

    private final TelephoneRepository telephoneRepository;
    private final UnitRepository unitRepository;

    public RegisterUnitTelephoneImpl(
            TelephoneRepository telephoneRepository,
            UnitRepository unitRepository) {
        this.telephoneRepository = telephoneRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    public RegisterTelephoneOutput execute(RegisterUnitTelephoneCommand command) {
        if (unitRepository.findById(command.unitId()).isEmpty()) {
            throw new UnitNotFoundException(command.unitId());
        }

        if (telephoneRepository.findByUnitId(command.unitId()).isPresent()) {
            throw TelephoneAlreadyRegisteredException.forUnit(command.unitId());
        }

        TelephoneNumber number = new TelephoneNumber(command.number());

        Telephone telephone = Telephone.forUnit(
                UUID.randomUUID(),
                number,
                command.unitId()
        );
        Telephone saved = telephoneRepository.save(telephone);

        return new RegisterTelephoneOutput(saved.getTelephoneId(), saved.getNumber().value());
    }
}
