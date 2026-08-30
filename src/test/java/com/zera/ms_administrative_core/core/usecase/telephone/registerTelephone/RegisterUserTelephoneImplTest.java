package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUserTelephoneImplTest {

    @Mock
    private TelephoneRepository telephoneRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private RegisterUserTelephoneImpl useCase;

    private final UUID userId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    private User user;
    private Unit unit;
    private RegisterUserTelephoneCommand command;

    @BeforeEach
    void setUp() {
        user = new Manager(userId, "User", new Email("user@email.com"), new HashedPassword("hash"),
                Status.ACTIVE, unitId, LocalDateTime.now(), LocalDateTime.now());
        unit = new Unit(unitId, "Matriz", organizationId);
        command = new RegisterUserTelephoneCommand(userId, "11987654321");
    }

    @Test
    @DisplayName("Should register a telephone for a user")
    void shouldRegister() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(telephoneRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(telephoneRepository.save(any(Telephone.class))).thenAnswer(i -> i.getArgument(0));

        RegisterTelephoneOutput output = useCase.execute(command);

        assertNotNull(output.telephoneId());
        assertEquals("11987654321", output.number());
        verify(telephoneRepository).save(any(Telephone.class));
    }

    @Test
    @DisplayName("Should fail when the user does not exist")
    void shouldFailWhenUserMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when the user's unit does not exist")
    void shouldFailWhenUnitMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when the user already has a telephone")
    void shouldFailWhenTelephoneAlreadyRegistered() {
        Telephone existing = new Telephone(UUID.randomUUID(), new TelephoneNumber("1133334444"),
                userId, organizationId, unitId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(telephoneRepository.findByUserId(userId)).thenReturn(Optional.of(existing));

        assertThrows(TelephoneAlreadyRegisteredException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }
}
