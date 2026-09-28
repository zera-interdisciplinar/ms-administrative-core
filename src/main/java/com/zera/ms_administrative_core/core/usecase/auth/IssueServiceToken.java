package com.zera.ms_administrative_core.core.usecase.auth;

public interface IssueServiceToken {
    ServiceToken execute(String clientId, String clientSecret);
}
