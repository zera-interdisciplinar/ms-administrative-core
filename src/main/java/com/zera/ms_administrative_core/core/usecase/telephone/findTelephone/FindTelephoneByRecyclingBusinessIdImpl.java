package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FindTelephoneByRecyclingBusinessIdImpl implements FindTelephoneByRecyclingBusinessId {

    private final TelephoneRepository repository;

    public FindTelephoneByRecyclingBusinessIdImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public TelephoneOutput execute(UUID recyclingBusinessId) {
        Telephone telephone = repository.findByRecyclingBusinessId(recyclingBusinessId)
                .orElseThrow(() -> new TelephoneNotFoundException(recyclingBusinessId));

        return TelephoneOutput.from(telephone);
    }
}
