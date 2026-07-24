package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import java.util.List;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;

public interface FindAllRecyclers {
    List<RecyclingBusiness> execute();
}
