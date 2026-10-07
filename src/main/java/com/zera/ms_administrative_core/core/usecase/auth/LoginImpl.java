package com.zera.ms_administrative_core.core.usecase.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.valueobject.AccessOrigin;

@Service
public class LoginImpl implements Login {

    private final AuthenticateUser authenticateUser;
    private final SessionTokenFactory sessionTokenFactory;

    public LoginImpl(AuthenticateUser authenticateUser, SessionTokenFactory sessionTokenFactory) {
        this.authenticateUser = authenticateUser;
        this.sessionTokenFactory = sessionTokenFactory;
    }

    // @Transactional nao e cerimonia: o SET LOCAL de zera.access_origin feito em
    // SessionTokenFactory so alcanca o INSERT do refresh token se os dois estiverem na mesma
    // transacao. Antes deste fix, este metodo nao tinha a anotacao e a origem nunca chegava.
    @Override
    @Transactional
    public TokenPair execute(String email, String rawPassword) {
        AuthenticatedUser user = authenticateUser.execute(email, rawPassword);
        return sessionTokenFactory.issueFor(user, AccessOrigin.LOGIN);
    }
}
