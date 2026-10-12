package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.security.Principal;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.usecase.devicetoken.registerDeviceToken.RegisterDeviceToken;
import com.zera.ms_administrative_core.core.usecase.devicetoken.unregisterDeviceToken.UnregisterDeviceToken;
import com.zera.ms_administrative_core.infrastructure.http.request.RegisterDeviceTokenRequest;
import com.zera.ms_administrative_core.infrastructure.http.request.UnregisterDeviceTokenRequest;

import jakarta.validation.Valid;

/**
 * Registro de device token FCM do proprio usuario autenticado, pra push de alerta (ver
 * {@code NotifyUserImpl}). Sempre o dono do token do request -- nunca um id na URL -- porque o
 * app so pode registrar/desregistrar o device em que esta rodando.
 */
@RestController
@RequestMapping("/api/v1/users/me/device-tokens")
public class DeviceTokenController {

    private final RegisterDeviceToken registerDeviceToken;
    private final UnregisterDeviceToken unregisterDeviceToken;

    public DeviceTokenController(RegisterDeviceToken registerDeviceToken, UnregisterDeviceToken unregisterDeviceToken) {
        this.registerDeviceToken = registerDeviceToken;
        this.unregisterDeviceToken = unregisterDeviceToken;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> register(Principal principal, @RequestBody @Valid RegisterDeviceTokenRequest request) {
        UUID userId = UUID.fromString(principal.getName());
        registerDeviceToken.execute(userId, request.token(), request.platform());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unregister(Principal principal, @RequestBody @Valid UnregisterDeviceTokenRequest request) {
        UUID userId = UUID.fromString(principal.getName());
        unregisterDeviceToken.execute(userId, request.token());
        return ResponseEntity.noContent().build();
    }
}
