package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

public interface findOrganizationByCnpj {
    OrganizationOutput execute(String cnpj);
}
