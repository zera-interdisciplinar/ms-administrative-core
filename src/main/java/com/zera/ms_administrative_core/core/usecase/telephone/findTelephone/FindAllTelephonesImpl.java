package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindAllTelephonesImpl implements FindAllTelephones {

    private final TelephoneRepository repository;

    public FindAllTelephonesImpl(TelephoneRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<TelephoneOutput> execute() {
        return repository.findAll().stream()
                .map(TelephoneOutput::from)
                .toList();
    }
}
