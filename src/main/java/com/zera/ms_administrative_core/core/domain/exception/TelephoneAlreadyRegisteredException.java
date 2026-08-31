package com.zera.ms_administrative_core.core.domain.exception;

import java.util.UUID;

public class TelephoneAlreadyRegisteredException extends RuntimeException {

    private TelephoneAlreadyRegisteredException(String message) {
        super(message);
    }

    public static TelephoneAlreadyRegisteredException forUser(UUID userId) {
        return new TelephoneAlreadyRegisteredException("Telephone already registered for user: " + userId);
    }

    public static TelephoneAlreadyRegisteredException forRecyclingBusiness(UUID recyclingBusinessId) {
        return new TelephoneAlreadyRegisteredException(
                "Telephone already registered for recycling business: " + recyclingBusinessId);
    }
}
