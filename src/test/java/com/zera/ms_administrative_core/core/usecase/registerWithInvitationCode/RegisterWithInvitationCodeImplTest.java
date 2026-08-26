package com.zera.ms_administrative_core.core.usecase.registerWithInvitationCode;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationExpiredException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationNotFoundException;
import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.repository.InvitationRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUserOutput;
import com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode.RegisterWithInvitationCodeCommand;
import com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode.RegisterWithInvitationCodeImpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterWithInvitationCodeImplTest {

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordHasher hasher;

    @InjectMocks
    private RegisterWithInvitationCodeImpl useCase;

    private UUID managerId;
    private UUID unitId;

    @BeforeEach
    void setUp() {
        managerId = UUID.randomUUID();
        unitId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Deve registrar funcionário e marcar convite como usado")
    void shouldRegisterEmployeeAndMarkInvitationUsed() {
        Invitation invitation = new Invitation(UUID.randomUUID(), "123456", managerId, unitId,
                LocalDateTime.now().plusHours(1));
        RegisterWithInvitationCodeCommand command = new RegisterWithInvitationCodeCommand(
                "123456", "Ana", "plain-password", "ana@example.com");

        when(invitationRepository.findPendingByCode("123456")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail(new Email("ana@example.com"))).thenReturn(false);
        when(hasher.hash(new RawPassword("plain-password"))).thenReturn(new HashedPassword("hashed"));

        RegisterUserOutput output = useCase.execute(command);

        assertEquals("Ana", output.name());
        assertEquals(Role.EMPLOYEE, output.role());
        assertEquals(managerId, output.managerId());
        verify(userRepository).save(any());
        verify(invitationRepository).save(invitation);
        assertEquals(com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus.USED,
                invitation.getStatus());
    }

    @Test
    @DisplayName("Deve lançar InvitationNotFoundException quando código não existe ou já foi usado")
    void shouldThrowWhenInvitationNotFound() {
        when(invitationRepository.findPendingByCode("999999")).thenReturn(Optional.empty());
        RegisterWithInvitationCodeCommand command = new RegisterWithInvitationCodeCommand(
                "999999", "Ana", "plain-password", "ana@example.com");

        assertThrows(InvitationNotFoundException.class, () -> useCase.execute(command));
    }

    @Test
    @DisplayName("Deve lançar InvitationExpiredException quando o código expirou")
    void shouldThrowWhenInvitationExpired() {
        Invitation invitation = new Invitation(UUID.randomUUID(), "123456", managerId, unitId,
                LocalDateTime.now().minusMinutes(1));
        when(invitationRepository.findPendingByCode("123456")).thenReturn(Optional.of(invitation));
        RegisterWithInvitationCodeCommand command = new RegisterWithInvitationCodeCommand(
                "123456", "Ana", "plain-password", "ana@example.com");

        assertThrows(InvitationExpiredException.class, () -> useCase.execute(command));
    }

    @Test
    @DisplayName("Deve lançar EmailAlreadyInUseException quando email já está em uso")
    void shouldThrowWhenEmailAlreadyInUse() {
        Invitation invitation = new Invitation(UUID.randomUUID(), "123456", managerId, unitId,
                LocalDateTime.now().plusHours(1));
        when(invitationRepository.findPendingByCode("123456")).thenReturn(Optional.of(invitation));
        when(userRepository.existsByEmail(new Email("ana@example.com"))).thenReturn(true);
        RegisterWithInvitationCodeCommand command = new RegisterWithInvitationCodeCommand(
                "123456", "Ana", "plain-password", "ana@example.com");

        assertThrows(EmailAlreadyInUseException.class, () -> useCase.execute(command));
    }
}
