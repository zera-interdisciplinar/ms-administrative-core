package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

public class FindRecyclingByIdImpl implements FindRecyclingById {
    private final RecyclingBusinessRepository repository;

    public FindRecyclingByIdImpl(RecyclingBusinessRepository repository){
        this.repository = repository;
    }

    @Override
    public Optional<RecyclingBusiness> execute(UUID id){
        Optional<RecyclingBusiness> recycling = repository.findById(id);
        return recycling;
    }
}
