package com.zera.ms_administrative_core.core.usecase.generateInvitationCode;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.InvitationRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode.GenerateInvitationCodeImpl;
import com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode.GenerateInvitationCodeOutput;
import com.zera.ms_administrative_core.util.UserTestFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateInvitationCodeImplTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GenerateInvitationCodeImpl useCase;

    private Manager manager;

    @BeforeEach
    void setUp() {
        manager = UserTestFactory.createManager();
    }

    @Test
    @DisplayName("Deve gerar um código de convite válido para o gestor")
    void shouldGenerateInvitationCode() {
        when(userRepository.findById(manager.getUserId())).thenReturn(Optional.of(manager));
        when(invitationRepository.findPendingByCode(any())).thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateInvitationCodeOutput output = useCase.execute(manager.getUserId());

        assertEquals(6, output.code().length());
        assertTrue(output.code().chars().allMatch(Character::isDigit));
        assertEquals(manager.getUserId(), output.managerId());
        assertEquals(manager.getUnitId(), output.unitId());
        verify(invitationRepository).save(any(Invitation.class));
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando o gestor não existe")
    void shouldThrowWhenManagerNotFound() {
        UUID managerId = UUID.randomUUID();
        when(userRepository.findById(managerId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(managerId));
    }

    @Test
    @DisplayName("Deve rejeitar quando o usuário informado não é um gestor")
    void shouldRejectWhenUserIsNotManager() {
        Employee employee = UserTestFactory.createEmployee();
        when(userRepository.findById(employee.getUserId())).thenReturn(Optional.of(employee));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(employee.getUserId()));
        assertEquals("Only managers can generate invitation codes", exception.getMessage());
    }

    @Test
    @DisplayName("Deve gerar novo código quando o primeiro já está em uso")
    void shouldRetryWhenCodeAlreadyPending() {
        when(userRepository.findById(manager.getUserId())).thenReturn(Optional.of(manager));
        when(invitationRepository.findPendingByCode(any()))
                .thenReturn(Optional.of(existingInvitation()))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(manager.getUserId());

        ArgumentCaptor<Invitation> captor = ArgumentCaptor.forClass(Invitation.class);
        verify(invitationRepository).save(captor.capture());
        assertTrue(captor.getValue().getCode().chars().allMatch(Character::isDigit));
    }

    @Test
    @DisplayName("Deve tentar novamente quando ocorre corrida de concorrência ao salvar")
    void shouldRetryOnConcurrentRace() {
        when(userRepository.findById(manager.getUserId())).thenReturn(Optional.of(manager));
        when(invitationRepository.findPendingByCode(any())).thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GenerateInvitationCodeOutput output = useCase.execute(manager.getUserId());

        assertEquals(manager.getUserId(), output.managerId());
    }

    private Invitation existingInvitation() {
        return new Invitation(UUID.randomUUID(), "000000", UUID.randomUUID(), UUID.randomUUID(),
                java.time.LocalDateTime.now().plusHours(1));
    }
}
