package com.zera.ms_administrative_core.core.usecase.recycling.registerRecycling;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;

public interface RegisterRecycling {
    RecyclingBusiness execute(String name, String cnpj, String email);
}
