package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FindTelephoneByUserIdImpl implements FindTelephoneByUserId {

    private final TelephoneRepository repository;

    public FindTelephoneByUserIdImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public TelephoneOutput execute(UUID userId) {
        Telephone telephone = repository.findByUserId(userId)
                .orElseThrow(() -> new TelephoneNotFoundException(userId));

        return TelephoneOutput.from(telephone);
    }
}
