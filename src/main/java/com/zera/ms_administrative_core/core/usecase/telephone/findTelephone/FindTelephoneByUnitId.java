package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import java.util.UUID;

public interface FindTelephoneByUnitId {
    TelephoneOutput execute(UUID unitId);
}
