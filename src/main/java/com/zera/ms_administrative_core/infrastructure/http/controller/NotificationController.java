package com.zera.ms_administrative_core.infrastructure.http.controller;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.usecase.notification.closeAlert.CloseAlertForUser;
import com.zera.ms_administrative_core.core.usecase.notification.enqueueAlert.EnqueueAlertNotification;
import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.AlertOutput;
import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.FindAlertsForUser;
import com.zera.ms_administrative_core.infrastructure.security.Authz;
import com.zera.ms_administrative_core.infrastructure.http.request.AlertNotificationRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
    private final EnqueueAlertNotification enqueueAlertNotification;
    private final FindAlertsForUser findAlertsForUser;
    private final CloseAlertForUser closeAlertForUser;

    public NotificationController(EnqueueAlertNotification enqueueAlertNotification,
                                   FindAlertsForUser findAlertsForUser,
                                   CloseAlertForUser closeAlertForUser) {
        this.enqueueAlertNotification = enqueueAlertNotification;
        this.findAlertsForUser = findAlertsForUser;
        this.closeAlertForUser = closeAlertForUser;
    }

    /**
     * Chamada de servico-para-servico (ms-inventory, IA core). Exige o escopo dedicado: token de
     * usuario nao traz {@code scope} e nao alcanca esta rota.
     *
     * <p>So enfileira (Redis Stream) e devolve 202 na hora; quem valida o usuario, deduplica e
     * grava o alerta e o worker que consome a fila (ver {@code infrastructure/queue/redis}), nao
     * esta requisicao. Isso evita que uma lentidao pontual do Postgres vire timeout pro
     * ms-inventory.
     */
    @PostMapping("/alerts")
    @PreAuthorize(Authz.SERVICE_NOTIFICATIONS)
    public ResponseEntity<Void> receiveAlert(@RequestBody @Valid AlertNotificationRequest request) {
        enqueueAlertNotification.execute(request.toCommand());
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

    /**
     * Marca o alerta como CLOSED a pedido do dono. O job de X dias continua existindo; isto e o
     * caminho do app. Sem corpo: so existe fechar, nao reabrir.
     */
    @PatchMapping("/alerts/{alertId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AlertOutput> closeAlert(Principal principal, @PathVariable UUID alertId) {
        UUID userId = UUID.fromString(principal.getName());
        return ResponseEntity.ok(closeAlertForUser.execute(userId, alertId));
    }
}
