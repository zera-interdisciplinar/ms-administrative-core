package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

public interface FindOrganizationByCnpj {
    OrganizationOutput execute(String cnpj);
}
