package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RegisterRecyclingTelephoneImpl implements RegisterRecyclingTelephone {

    private final TelephoneRepository telephoneRepository;
    private final RecyclingBusinessRepository recyclingBusinessRepository;

    public RegisterRecyclingTelephoneImpl(
            TelephoneRepository telephoneRepository,
            RecyclingBusinessRepository recyclingBusinessRepository) {
        this.telephoneRepository = telephoneRepository;
        this.recyclingBusinessRepository = recyclingBusinessRepository;
    }

    @Override
    public RegisterTelephoneOutput execute(RegisterRecyclingTelephoneCommand command) {
        if (recyclingBusinessRepository.findById(command.recyclingBusinessId()).isEmpty()) {
            throw new RecyclingNotFoundException(command.recyclingBusinessId());
        }

        if (telephoneRepository.findByRecyclingBusinessId(command.recyclingBusinessId()).isPresent()) {
            throw TelephoneAlreadyRegisteredException.forRecyclingBusiness(command.recyclingBusinessId());
        }

        TelephoneNumber number = new TelephoneNumber(command.number());

        Telephone telephone = new Telephone(
                UUID.randomUUID(),
                number,
                command.recyclingBusinessId()
        );
        Telephone saved = telephoneRepository.save(telephone);

        return new RegisterTelephoneOutput(saved.getTelephoneId(), saved.getNumber().value());
    }
}
