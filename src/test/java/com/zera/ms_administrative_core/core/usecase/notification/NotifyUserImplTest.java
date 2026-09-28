package com.zera.ms_administrative_core.core.usecase.notification;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.UserFactory;
import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.AlertRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotifyUserImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    private NotifyUserImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new NotifyUserImpl(alertRepository, userRepository);
    }

    @Test
    @DisplayName("Deve criar e salvar um alerta quando o usuário existe")
    void shouldCreateAndSaveAlertWhenUserExists() {
        UUID userId = UUID.randomUUID();
        User user = UserFactory.create(Role.EMPLOYEE, userId, "Bob", new Email("bob@example.com"),
                new HashedPassword("hash"), Status.ACTIVE, UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        NotifyUserCommand command = new NotifyUserCommand(
                UUID.randomUUID(), UUID.randomUUID(), userId, UUID.randomUUID(),
                "storage almost full", Severity.HIGH, AlertKind.STORAGE, AlertStatus.OPEN,
                LocalDateTime.of(2024, 1, 1, 8, 0));

        useCase.execute(command);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());

        Alert saved = captor.getValue();
        assertEquals(Severity.HIGH, saved.getSeverity());
        assertEquals(AlertKind.STORAGE, saved.getKind());
        assertEquals(userId, saved.getUserId());
        assertEquals("storage almost full", saved.getDescription());
        assertEquals(AlertStatus.OPEN, saved.getStatus());
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando o usuário não existe")
    void shouldThrowWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        NotifyUserCommand command = new NotifyUserCommand(
                UUID.randomUUID(), UUID.randomUUID(), userId, UUID.randomUUID(),
                "desc", Severity.LOW, AlertKind.TIME, AlertStatus.OPEN, LocalDateTime.now());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(command));

        verify(alertRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve atualizar o alerta OPEN existente em vez de criar um duplicado quando ruleId/eventId já foram alertados")
    void shouldRefreshExistingAlertInsteadOfDuplicating() {
        UUID userId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        User user = UserFactory.create(Role.EMPLOYEE, userId, "Bob", new Email("bob@example.com"),
                new HashedPassword("hash"), Status.ACTIVE, UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Alert existing = new Alert(AlertKind.STORAGE, Severity.LOW, "storage almost full", userId, ruleId, eventId,
                UUID.randomUUID(), AlertStatus.OPEN, UUID.randomUUID());
        when(alertRepository.findOpenByRuleIdAndEventId(ruleId, eventId)).thenReturn(Optional.of(existing));

        NotifyUserCommand command = new NotifyUserCommand(
                eventId, ruleId, userId, UUID.randomUUID(),
                "storage almost full", Severity.LOW, AlertKind.STORAGE, AlertStatus.OPEN,
                LocalDateTime.of(2024, 1, 1, 8, 0));

        useCase.execute(command);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertEquals(existing.getId(), captor.getValue().getId());
        assertEquals(Severity.LOW, captor.getValue().getSeverity());
    }

    @Test
    @DisplayName("Deve escalar a severidade do alerta existente quando a nova é maior")
    void shouldEscalateSeverityWhenNewSeverityIsHigher() {
        UUID userId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        User user = UserFactory.create(Role.EMPLOYEE, userId, "Bob", new Email("bob@example.com"),
                new HashedPassword("hash"), Status.ACTIVE, UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        Alert existing = new Alert(AlertKind.STORAGE, Severity.LOW, "storage almost full", userId, ruleId, eventId,
                UUID.randomUUID(), AlertStatus.OPEN, UUID.randomUUID());
        when(alertRepository.findOpenByRuleIdAndEventId(ruleId, eventId)).thenReturn(Optional.of(existing));

        NotifyUserCommand command = new NotifyUserCommand(
                eventId, ruleId, userId, UUID.randomUUID(),
                "storage almost full", Severity.HIGH, AlertKind.STORAGE, AlertStatus.OPEN,
                LocalDateTime.of(2024, 1, 1, 8, 0));

        useCase.execute(command);

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        assertEquals(Severity.HIGH, captor.getValue().getSeverity());
    }

    /**
     * O momento informado por quem detectou o alerta era aceito e descartado: o alerta gravava a
     * hora da chamada. Para o ms-inventory isso apagaria quando a regra disparou de fato.
     */
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Deve gravar o occurredAt informado em vez da hora da chamada")
    void shouldKeepTheInformedMoment() {
        java.util.UUID userId = java.util.UUID.randomUUID();
        java.time.LocalDateTime quando = java.time.LocalDateTime.of(2026, 9, 20, 3, 15);
        org.mockito.Mockito.when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(org.mockito.Mockito.mock(
                        com.zera.ms_administrative_core.core.domain.entity.User.class)));

        new NotifyUserImpl(alertRepository, userRepository).execute(new NotifyUserCommand(
                null, null, userId, java.util.UUID.randomUUID(), "garantia vencendo",
                com.zera.ms_administrative_core.core.domain.entity.Severity.MEDIUM,
                com.zera.ms_administrative_core.core.domain.entity.AlertKind.WARRANTY_EXPIRATION,
                com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus.OPEN, quando));

        org.mockito.ArgumentCaptor<com.zera.ms_administrative_core.core.domain.entity.Alert> salvo =
                org.mockito.ArgumentCaptor.forClass(
                        com.zera.ms_administrative_core.core.domain.entity.Alert.class);
        org.mockito.Mockito.verify(alertRepository).save(salvo.capture());
        org.junit.jupiter.api.Assertions.assertEquals(quando, salvo.getValue().getOccurredAt());
        org.junit.jupiter.api.Assertions.assertEquals(
                com.zera.ms_administrative_core.core.domain.entity.AlertKind.WARRANTY_EXPIRATION,
                salvo.getValue().getKind());
    }

    /** Sem momento informado, vale a hora em que o alerta chegou. */
    @org.junit.jupiter.api.Test
    @org.junit.jupiter.api.DisplayName("Deve cair na hora atual quando o occurredAt nao vier")
    void shouldFallBackToNowWithoutAMoment() {
        java.util.UUID userId = java.util.UUID.randomUUID();
        org.mockito.Mockito.when(userRepository.findById(userId))
                .thenReturn(java.util.Optional.of(org.mockito.Mockito.mock(
                        com.zera.ms_administrative_core.core.domain.entity.User.class)));

        new NotifyUserImpl(alertRepository, userRepository).execute(new NotifyUserCommand(
                null, null, userId, java.util.UUID.randomUUID(), "item aprovado",
                com.zera.ms_administrative_core.core.domain.entity.Severity.LOW,
                com.zera.ms_administrative_core.core.domain.entity.AlertKind.ITEM_APPROVED,
                com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus.OPEN, null));

        org.mockito.ArgumentCaptor<com.zera.ms_administrative_core.core.domain.entity.Alert> salvo =
                org.mockito.ArgumentCaptor.forClass(
                        com.zera.ms_administrative_core.core.domain.entity.Alert.class);
        org.mockito.Mockito.verify(alertRepository).save(salvo.capture());
        org.junit.jupiter.api.Assertions.assertNotNull(salvo.getValue().getOccurredAt());
    }
}
