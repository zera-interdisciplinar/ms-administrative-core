package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class FindTelephoneByUnitIdImpl implements FindTelephoneByUnitId {

    private final TelephoneRepository repository;

    public FindTelephoneByUnitIdImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public TelephoneOutput execute(UUID unitId) {
        Telephone telephone = repository.findByUnitId(unitId)
                .orElseThrow(() -> new TelephoneNotFoundException(unitId));

        return TelephoneOutput.from(telephone);
    }
}
