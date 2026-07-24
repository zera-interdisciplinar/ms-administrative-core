package com.zera.ms_administrative_core.core.usecase.recycling.renameRecycling;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

import org.springframework.stereotype.Service;

@Service("renameRecycling")
public class RenameRecyclingImpl implements RenameRecycling {
    private final RecyclingBusinessRepository recyclingBusinessRepository;

    public RenameRecyclingImpl(RecyclingBusinessRepository recyclingBusinessRepository) {
        this.recyclingBusinessRepository = recyclingBusinessRepository;
    }

    @Override
    public void execute(UUID recyclingId, String name) {
        RecyclingBusiness recyclingBusiness = recyclingBusinessRepository.findById(recyclingId)
            .orElseThrow(() -> new RecyclingNotFoundException(recyclingId));
        recyclingBusiness.rename(name);
        recyclingBusinessRepository.save(recyclingBusiness);
    }
}
