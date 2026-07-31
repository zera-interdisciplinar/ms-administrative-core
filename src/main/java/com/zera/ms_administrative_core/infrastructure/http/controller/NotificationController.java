package com.zera.ms_administrative_core.infrastructure.http.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.infrastructure.http.request.AlertNotificationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotifyUser notifyUser;

    public NotificationController(NotifyUser notifyUser){
        this.notifyUser = notifyUser;
    }

    @PostMapping("/alerts")
    public ResponseEntity<Void> receiveAlert(@RequestBody @Valid AlertNotificationRequest request) {
        notifyUser.execute(request.toCommand());
        return ResponseEntity.accepted().build();
    }
}
