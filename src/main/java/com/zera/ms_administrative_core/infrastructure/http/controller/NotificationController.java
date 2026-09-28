package com.zera.ms_administrative_core.infrastructure.http.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.infrastructure.security.Authz;
import com.zera.ms_administrative_core.infrastructure.http.request.AlertNotificationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotifyUser notifyUser;

    public NotificationController(NotifyUser notifyUser){
        this.notifyUser = notifyUser;
    }

    /**
     * Chamada de servico-para-servico (ms-inventory, IA core). Exige o escopo dedicado: token de
     * usuario nao traz {@code scope} e nao alcanca esta rota.
     */
    @PostMapping("/alerts")
    @PreAuthorize(Authz.SERVICE_NOTIFICATIONS)
    public ResponseEntity<Void> receiveAlert(@RequestBody @Valid AlertNotificationRequest request) {
        notifyUser.execute(request.toCommand());
        return ResponseEntity.accepted().build();
    }
}
