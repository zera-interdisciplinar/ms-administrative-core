package com.zera.ms_administrative_core.infrastructure.http.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.usecase.auth.IssueServiceToken;
import com.zera.ms_administrative_core.core.usecase.auth.Login;
import com.zera.ms_administrative_core.core.usecase.auth.Logout;
import com.zera.ms_administrative_core.core.usecase.auth.RefreshSession;
import com.zera.ms_administrative_core.infrastructure.http.request.LoginRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.ServiceTokenRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.RefreshTokenRequest;
import com.zera.ms_administrative_core.infrastructure.http.response.ServiceTokenResponse;
import com.zera.ms_administrative_core.infrastructure.http.response.TokenResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final Login login;
    private final RefreshSession refreshSession;
    private final Logout logout;

    private final IssueServiceToken issueServiceToken;

    public AuthController(Login login, RefreshSession refreshSession, Logout logout,
            IssueServiceToken issueServiceToken) {
        this.issueServiceToken = issueServiceToken;
        this.login = login;
        this.refreshSession = refreshSession;
        this.logout = logout;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(TokenResponse.from(
                login.execute(request.email(), request.password())));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@RequestBody @Valid RefreshTokenRequest request) {
        return ResponseEntity.ok(TokenResponse.from(
                refreshSession.execute(request.refreshToken())));
    }

    /**
     * Token de servico para as rotas internas. Nao devolve refresh token: o cliente pede outro
     * quando este expirar, com as mesmas credenciais.
     */
    @PostMapping("/service-token")
    public ResponseEntity<ServiceTokenResponse> serviceToken(@RequestBody @Valid ServiceTokenRequest request) {
        return ResponseEntity.ok(ServiceTokenResponse.from(
                issueServiceToken.execute(request.clientId(), request.clientSecret())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequest request) {
        logout.execute(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
