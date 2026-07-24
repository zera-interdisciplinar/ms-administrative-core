package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.Optional;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;

public interface FindRecyclingByCnpj {
    Optional<RecyclingBusiness> execute(String cnpj);
}