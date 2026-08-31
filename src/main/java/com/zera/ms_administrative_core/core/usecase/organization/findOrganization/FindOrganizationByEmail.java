package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

public interface FindOrganizationByEmail {
    OrganizationOutput execute(String email);
}
