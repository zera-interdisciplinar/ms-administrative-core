package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.Optional;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

public class FindRecyclingByCnpjImpl implements FindRecyclingByCnpj{
    private final RecyclingBusinessRepository repo;

    public FindRecyclingByCnpjImpl(RecyclingBusinessRepository repo){
        this.repo = repo;
    }

    @Override
    public Optional<RecyclingBusiness> execute(String cnpj){
        Optional<RecyclingBusiness> recycling = repo.findByCnpj(new Cnpj(cnpj));
        return recycling;
    }
}
