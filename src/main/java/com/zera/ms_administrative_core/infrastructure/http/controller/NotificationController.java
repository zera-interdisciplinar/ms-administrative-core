package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.AlertOutput;
import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.FindAlertsForUser;
import com.zera.ms_administrative_core.infrastructure.security.Authz;
import com.zera.ms_administrative_core.infrastructure.http.request.AlertNotificationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final NotifyUser notifyUser;
    private final FindAlertsForUser findAlertsForUser;

    public NotificationController(NotifyUser notifyUser, FindAlertsForUser findAlertsForUser) {
        this.notifyUser = notifyUser;
        this.findAlertsForUser = findAlertsForUser;
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

    /**
     * O alerta ja nasce vinculado ao destinatario (userId); cada usuario ve so os proprios, entao
     * o filtro vem do token (sub), nunca de um parametro que o cliente poderia trocar.
     */
    @GetMapping("/alerts")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AlertOutput>> findAlerts(Principal principal,
                                                         @RequestParam(required = false) AlertStatus status,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        UUID userId = UUID.fromString(principal.getName());
        return ResponseEntity.ok(findAlertsForUser.execute(userId, status, page, size));
    }
}
