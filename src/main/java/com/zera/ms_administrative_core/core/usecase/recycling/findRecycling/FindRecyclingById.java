package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;

public interface FindRecyclingById {
    RecyclingBusiness execute (UUID id); 
}
