package com.zera.ms_administrative_core.core.usecase.telephone.registerTelephone;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.RecyclingNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneAlreadyRegisteredException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.RecyclingBusinessRepository;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
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
class RegisterRecyclingTelephoneImplTest {

    @Mock
    private TelephoneRepository telephoneRepository;
    @Mock
    private RecyclingBusinessRepository recyclingBusinessRepository;

    @InjectMocks
    private RegisterRecyclingTelephoneImpl useCase;

    private final UUID recyclingId = UUID.randomUUID();
    private RegisterRecyclingTelephoneCommand command;
    private RecyclingBusiness business;

    @BeforeEach
    void setUp() {
        command = new RegisterRecyclingTelephoneCommand(recyclingId, "11987654321");
        business = new RecyclingBusiness(recyclingId, "Recycler",
                new Cnpj("11.222.333/0001-81"), new Email("recycler@email.com"));
    }

    @Test
    @DisplayName("Should register a telephone for a recycling business")
    void shouldRegister() {
        when(recyclingBusinessRepository.findById(recyclingId)).thenReturn(Optional.of(business));
        when(telephoneRepository.findByRecyclingBusinessId(recyclingId)).thenReturn(Optional.empty());
        when(telephoneRepository.save(any(Telephone.class))).thenAnswer(i -> i.getArgument(0));

        RegisterTelephoneOutput output = useCase.execute(command);

        assertEquals("11987654321", output.number());
        verify(telephoneRepository).save(any(Telephone.class));
    }

    @Test
    @DisplayName("Should fail when the recycling business does not exist")
    void shouldFailWhenRecyclingMissing() {
        when(recyclingBusinessRepository.findById(recyclingId)).thenReturn(Optional.empty());

        assertThrows(RecyclingNotFoundException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should fail when the recycling business already has a telephone")
    void shouldFailWhenAlreadyRegistered() {
        Telephone existing = new Telephone(UUID.randomUUID(), new TelephoneNumber("1133334444"), recyclingId);
        when(recyclingBusinessRepository.findById(recyclingId)).thenReturn(Optional.of(business));
        when(telephoneRepository.findByRecyclingBusinessId(recyclingId)).thenReturn(Optional.of(existing));

        assertThrows(TelephoneAlreadyRegisteredException.class, () -> useCase.execute(command));
        verify(telephoneRepository, never()).save(any());
    }
}
