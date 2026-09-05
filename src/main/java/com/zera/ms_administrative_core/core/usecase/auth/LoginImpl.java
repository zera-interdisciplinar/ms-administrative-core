package com.zera.ms_administrative_core.core.usecase.auth;

import org.springframework.stereotype.Service;

@Service
public class LoginImpl implements Login {

    private final AuthenticateUser authenticateUser;
    private final SessionTokenFactory sessionTokenFactory;

    public LoginImpl(AuthenticateUser authenticateUser, SessionTokenFactory sessionTokenFactory) {
        this.authenticateUser = authenticateUser;
        this.sessionTokenFactory = sessionTokenFactory;
    }

    @Override
    public TokenPair execute(String email, String rawPassword) {
        AuthenticatedUser user = authenticateUser.execute(email, rawPassword);
        return sessionTokenFactory.issueFor(user);
    }
}
