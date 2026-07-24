package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.List;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

import org.springframework.stereotype.Service;

@Service("findAllRecyclers")
public class FindAllRecyclersImpl implements FindAllRecyclers {
    private final RecyclingBusinessRepository repo;

    public FindAllRecyclersImpl(RecyclingBusinessRepository repo){
        this.repo = repo;
    }

    @Override
    public List<RecyclingBusiness> execute(){
        List<RecyclingBusiness> list = repo.findAll();
        return list;
    }
}
