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

    public static TelephoneAlreadyRegisteredException forOrganization(UUID organizationId) {
        return new TelephoneAlreadyRegisteredException("Telephone already registered for organization: " + organizationId);
    }

    public static TelephoneAlreadyRegisteredException forUnit(UUID unitId) {
        return new TelephoneAlreadyRegisteredException("Telephone already registered for unit: " + unitId);
    }
}
