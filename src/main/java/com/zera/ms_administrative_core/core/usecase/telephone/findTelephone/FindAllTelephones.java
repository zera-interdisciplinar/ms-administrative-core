package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import java.util.List;

public interface FindAllTelephones {
    List<TelephoneOutput> execute(int page, int size);
}
