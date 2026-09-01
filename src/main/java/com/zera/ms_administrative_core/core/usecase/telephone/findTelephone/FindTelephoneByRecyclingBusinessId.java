package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import java.util.UUID;

public interface FindTelephoneByRecyclingBusinessId {
    TelephoneOutput execute(UUID recyclingBusinessId);
}
