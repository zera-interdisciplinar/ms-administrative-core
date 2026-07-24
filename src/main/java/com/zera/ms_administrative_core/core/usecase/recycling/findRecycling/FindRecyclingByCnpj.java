package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;

public interface FindRecyclingByCnpj {
    RecyclingBusiness execute(String cnpj);
}