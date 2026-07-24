package com.zera.ms_administrative_core.core.usecase.recycling.registerRecycling;

import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

public interface RegisterRecycling {
    void execute(String name, Cnpj cnpj, Email email);
}
