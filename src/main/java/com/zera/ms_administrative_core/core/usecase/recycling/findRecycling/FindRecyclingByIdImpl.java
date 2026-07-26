package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

import org.springframework.stereotype.Service;

@Service("findRecyclingById")
public class FindRecyclingByIdImpl implements FindRecyclingById {
    private final RecyclingBusinessRepository repository;

    public FindRecyclingByIdImpl(RecyclingBusinessRepository repository){
        this.repository = repository;
    }

    @Override
    public RecyclingBusiness execute(UUID id){
        RecyclingBusiness recycling = repository.findById(id).orElseThrow(() -> new RecyclingNotFoundException(id));
        return recycling;
    }
}
