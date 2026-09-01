package com.zera.ms_administrative_core.core.usecase.telephone.changeTelephone;

import java.util.UUID;

public interface ChangeTelephone {
    void execute(UUID telephoneId, String newNumber);
}
