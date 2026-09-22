package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterUnitTelephoneImplTest {

    @Mock
    private TelephoneRepository telephoneRepository;
    @Mock
    private UnitRepository unitRepository;

    @InjectMocks
    private RegisterUnitTelephoneImpl useCase;

    private final UUID unitId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private RegisterUnitTelephoneCommand command;
    private Unit unit;

    @BeforeEach
    void setUp() {
        command = new RegisterUnitTelephoneCommand(unitId, "11987654321");
        unit = new Unit(unitId, "Matriz", organizationId);
    }

    @Test
    @DisplayName("Should register a telephone for a unit")
    void shouldRegister() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(telephoneRepository.findByUnitId(unitId)).thenReturn(Optional.empty());
        when(telephoneRepository.save(any(Telephone.class))).thenAnswer(i -> i.getArgument(0));

        RegisterTelephoneOutput output = useCase.execute(command);

        assertEquals("11987654321", output.number());
        verify(telephoneRepository).save(any(Telephone.class));
    }

    @Test
    @DisplayName("Should fail when the unit does not exist")
    void shouldFailWhenUnitMissing() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when the unit already has a telephone")
    void shouldFailWhenAlreadyRegistered() {
        Telephone existing = Telephone.forUnit(UUID.randomUUID(), new TelephoneNumber("1133334444"), unitId);
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));
        when(telephoneRepository.findByUnitId(unitId)).thenReturn(Optional.of(existing));

        assertThrows(TelephoneAlreadyRegisteredException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }
}
