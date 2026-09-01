package com.zera.ms_administrative_core.core.usecase.telephone.deleteTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DeleteTelephoneImpl implements DeleteTelephone {

    private final TelephoneRepository repository;

    public DeleteTelephoneImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID telephoneId) {
        Telephone telephone = repository.findById(telephoneId)
                .orElseThrow(() -> new TelephoneNotFoundException(telephoneId));

        repository.delete(telephone);
    }
}
