package com.zera.ms_administrative_core.core.usecase.telephone.changeTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ChangeTelephoneImpl implements ChangeTelephone {

    private final TelephoneRepository repository;

    public ChangeTelephoneImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID telephoneId, String newNumber) {
        Telephone telephone = repository.findById(telephoneId)
                .orElseThrow(() -> new TelephoneNotFoundException(telephoneId));

        TelephoneNumber number = new TelephoneNumber(newNumber);

        if (telephone.getNumber().equals(number)) {
            return;
        }

        telephone.changeNumber(number);
        repository.save(telephone);
    }
}
