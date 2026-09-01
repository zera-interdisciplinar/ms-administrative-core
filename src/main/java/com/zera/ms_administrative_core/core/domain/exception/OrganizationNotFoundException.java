package com.zera.ms_administrative_core.core.domain.exception;

import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

import java.util.UUID;

public class OrganizationNotFoundException extends RuntimeException {
    public OrganizationNotFoundException(UUID id) {
        super("Organization not found: " + id);
    }

    public OrganizationNotFoundException(Cnpj cnpj) {
        super("Organization not found: " + cnpj.value());
    }

    public OrganizationNotFoundException(Email email) {
        super("Organization not found: " + email);
    }
}
