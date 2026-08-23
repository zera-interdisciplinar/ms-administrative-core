package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

public interface findOrganizationByEmail {
    OrganizationOutput execute(String email);
}
