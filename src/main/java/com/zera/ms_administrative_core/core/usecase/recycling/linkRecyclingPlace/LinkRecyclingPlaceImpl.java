package com.zera.ms_administrative_core.core.usecase.recycling.linkRecyclingPlace;

import java.util.List;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingPlaceAlreadyLinkedException;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;

import org.springframework.stereotype.Service;

@Service("linkRecyclingPlace")
public class LinkRecyclingPlaceImpl implements LinkRecyclingPlace {

    private final RecyclingBusinessRepository recyclingBusinessRepository;

    public LinkRecyclingPlaceImpl(RecyclingBusinessRepository recyclingBusinessRepository) {
        this.recyclingBusinessRepository = recyclingBusinessRepository;
    }

    // A checagem antes do save deixa o erro claro (409); o indice unico no banco fica como rede
    // de seguranca para a corrida entre duas requisicoes simultaneas.
    @Override
    public void execute(UUID recyclingBusinessId, String placeId) {
        RecyclingBusiness business = recyclingBusinessRepository.findById(recyclingBusinessId)
                .orElseThrow(() -> new RecyclingNotFoundException(recyclingBusinessId));

        boolean linkedToOther = recyclingBusinessRepository.findByPlaceIdIn(List.of(placeId)).stream()
                .anyMatch(other -> !other.getId().equals(recyclingBusinessId));
        if (linkedToOther) {
            throw new RecyclingPlaceAlreadyLinkedException(placeId);
        }

        business.linkPlace(placeId);
        recyclingBusinessRepository.save(business);
    }
}
