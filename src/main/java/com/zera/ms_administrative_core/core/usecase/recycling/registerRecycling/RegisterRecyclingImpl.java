package com.zera.ms_administrative_core.core.usecase.recycling.registerRecycling;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

public class RegisterRecyclingImpl implements RegisterRecycling {
    private final RecyclingBusinessRepository recyclingBusinessRepository;

    public RegisterRecyclingImpl(RecyclingBusinessRepository recyclingBusinessRepository) {
        this.recyclingBusinessRepository = recyclingBusinessRepository;
    }

    @Override
    public RecyclingBusiness execute(String name, String cnpj, String email) {
        Cnpj voCnpj = new Cnpj(cnpj);
        Email voEmail = new Email(email);

        RecyclingBusiness recyclingBusiness = new RecyclingBusiness(UUID.randomUUID(), name, voCnpj, voEmail);
        return recyclingBusinessRepository.save(recyclingBusiness);
    }
}
