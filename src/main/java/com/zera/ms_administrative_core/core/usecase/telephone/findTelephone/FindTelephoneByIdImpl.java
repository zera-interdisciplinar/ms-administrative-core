package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FindTelephoneByIdImpl implements FindTelephoneById {

    private final TelephoneRepository repository;

    public FindTelephoneByIdImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public TelephoneOutput execute(UUID telephoneId) {
        Telephone telephone = repository.findById(telephoneId)
                .orElseThrow(() -> new TelephoneNotFoundException(telephoneId));

        return TelephoneOutput.from(telephone);
    }
}
