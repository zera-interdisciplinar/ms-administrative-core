package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

public class FindRecyclingByCnpjImpl implements FindRecyclingByCnpj{
    private final RecyclingBusinessRepository repo;

    public FindRecyclingByCnpjImpl(RecyclingBusinessRepository repo){
        this.repo = repo;
    }

    @Override
    public RecyclingBusiness execute(String cnpj){
        RecyclingBusiness recycling = repo.findByCnpj(new Cnpj(cnpj)).orElseThrow(() -> new RecyclingNotFoundException(cnpj));
        return recycling;
    }
}
