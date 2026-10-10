package com.zera.ms_administrative_core.core.usecase.recycling.findRecycling;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;

import org.springframework.stereotype.Service;

@Service
public class FindRecyclingContactByPlaceIdImpl implements FindRecyclingContactByPlaceId {

    private final RecyclingBusinessRepository recyclings;
    private final TelephoneRepository telephones;

    public FindRecyclingContactByPlaceIdImpl(RecyclingBusinessRepository recyclings,
                                             TelephoneRepository telephones) {
        this.recyclings = recyclings;
        this.telephones = telephones;
    }

    /**
     * O inventory pergunta pelo placeId do agendamento. Sem ficha vinculada e 404; telefone
     * ausente nao e erro, porque a ficha pode existir so com e-mail.
     */
    @Override
    public RecyclingContact execute(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            throw new IllegalArgumentException("placeId is required");
        }
        String normalized = placeId.strip();
        if (normalized.length() > 200) {
            throw new IllegalArgumentException("placeId must have at most 200 characters");
        }
        RecyclingBusiness business = recyclings.findByPlaceId(normalized)
                .orElseThrow(() -> new RecyclingNotFoundException(normalized));
        String phone = telephones.findByRecyclingBusinessId(business.getId())
                .map(telephone -> telephone.getNumber().value())
                .orElse(null);
        return new RecyclingContact(business.getId(), business.getName(), business.getEmail().value(), phone);
    }
}
