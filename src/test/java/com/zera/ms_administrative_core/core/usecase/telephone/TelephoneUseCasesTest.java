package com.zera.ms_administrative_core.core.usecase.telephone;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.exception.TelephoneNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.core.repository.TelephoneRepository;
import com.zera.ms_administrative_core.core.usecase.telephone.changeTelephone.ChangeTelephoneImpl;
import com.zera.ms_administrative_core.core.usecase.telephone.deleteTelephone.DeleteTelephoneImpl;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindAllTelephonesByOrganizationIdImpl;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindAllTelephonesImpl;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindTelephoneByIdImpl;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindTelephoneByRecyclingBusinessIdImpl;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.FindTelephoneByUserIdImpl;
import com.zera.ms_administrative_core.core.usecase.telephone.findTelephone.TelephoneOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelephoneUseCasesTest {

    @Mock
    private TelephoneRepository repository;

    private final UUID telephoneId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final UUID recyclingId = UUID.randomUUID();

    private Telephone userTelephone;
    private Telephone recyclingTelephone;

    @BeforeEach
    void setUp() {
        userTelephone = new Telephone(telephoneId, new TelephoneNumber("11987654321"),
                userId, organizationId, unitId);
        recyclingTelephone = new Telephone(telephoneId, new TelephoneNumber("11987654321"), recyclingId);
    }

    // --- ChangeTelephone ---

    @Test
    @DisplayName("ChangeTelephone should change the number and persist")
    void shouldChangeNumber() {
        when(repository.findById(telephoneId)).thenReturn(Optional.of(userTelephone));

        new ChangeTelephoneImpl(repository).execute(telephoneId, "1133334444");

        assertEquals(new TelephoneNumber("1133334444"), userTelephone.getNumber());
        verify(repository).save(userTelephone);
    }

    @Test
    @DisplayName("ChangeTelephone should be a no-op when the number is unchanged")
    void shouldSkipChangeWhenSameNumber() {
        when(repository.findById(telephoneId)).thenReturn(Optional.of(userTelephone));

        new ChangeTelephoneImpl(repository).execute(telephoneId, "11987654321");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("ChangeTelephone should fail when the telephone does not exist")
    void shouldFailChangeWhenMissing() {
        when(repository.findById(telephoneId)).thenReturn(Optional.empty());

        assertThrows(TelephoneNotFoundException.class,
                () -> new ChangeTelephoneImpl(repository).execute(telephoneId, "1133334444"));
    }

    // --- DeleteTelephone ---

    @Test
    @DisplayName("DeleteTelephone should delete an existing telephone")
    void shouldDelete() {
        when(repository.findById(telephoneId)).thenReturn(Optional.of(userTelephone));

        new DeleteTelephoneImpl(repository).execute(telephoneId);

        verify(repository).delete(userTelephone);
    }

    @Test
    @DisplayName("DeleteTelephone should fail when the telephone does not exist")
    void shouldFailDeleteWhenMissing() {
        when(repository.findById(telephoneId)).thenReturn(Optional.empty());

        assertThrows(TelephoneNotFoundException.class,
                () -> new DeleteTelephoneImpl(repository).execute(telephoneId));
        verify(repository, never()).delete(any());
    }

    // --- FindTelephone* ---

    @Test
    @DisplayName("FindTelephoneById should return the mapped output")
    void shouldFindById() {
        when(repository.findById(telephoneId)).thenReturn(Optional.of(userTelephone));

        TelephoneOutput output = new FindTelephoneByIdImpl(repository).execute(telephoneId);

        assertEquals(telephoneId, output.telephoneId());
        assertEquals(userId, output.userId());
    }

    @Test
    @DisplayName("FindTelephoneById should fail when not found")
    void shouldFailFindById() {
        when(repository.findById(telephoneId)).thenReturn(Optional.empty());

        assertThrows(TelephoneNotFoundException.class,
                () -> new FindTelephoneByIdImpl(repository).execute(telephoneId));
    }

    @Test
    @DisplayName("FindTelephoneByUserId should return the mapped output")
    void shouldFindByUserId() {
        when(repository.findByUserId(userId)).thenReturn(Optional.of(userTelephone));

        TelephoneOutput output = new FindTelephoneByUserIdImpl(repository).execute(userId);

        assertEquals(userId, output.userId());
    }

    @Test
    @DisplayName("FindTelephoneByUserId should fail when not found")
    void shouldFailFindByUserId() {
        when(repository.findByUserId(userId)).thenReturn(Optional.empty());

        assertThrows(TelephoneNotFoundException.class,
                () -> new FindTelephoneByUserIdImpl(repository).execute(userId));
    }

    @Test
    @DisplayName("FindTelephoneByRecyclingBusinessId should return the mapped output")
    void shouldFindByRecyclingId() {
        when(repository.findByRecyclingBusinessId(recyclingId)).thenReturn(Optional.of(recyclingTelephone));

        TelephoneOutput output = new FindTelephoneByRecyclingBusinessIdImpl(repository).execute(recyclingId);

        assertEquals(recyclingId, output.recyclingBusinessId());
    }

    @Test
    @DisplayName("FindTelephoneByRecyclingBusinessId should fail when not found")
    void shouldFailFindByRecyclingId() {
        when(repository.findByRecyclingBusinessId(recyclingId)).thenReturn(Optional.empty());

        assertThrows(TelephoneNotFoundException.class,
                () -> new FindTelephoneByRecyclingBusinessIdImpl(repository).execute(recyclingId));
    }

    @Test
    @DisplayName("FindAllTelephones should map every telephone returned by the repository")
    void shouldFindAll() {
        when(repository.findAll(0, 20)).thenReturn(List.of(userTelephone));

        List<TelephoneOutput> output = new FindAllTelephonesImpl(repository).execute(0, 20);

        assertEquals(1, output.size());
    }

    @Test
    @DisplayName("FindAllTelephonesByOrganizationId should map every telephone returned by the repository")
    void shouldFindAllByOrganization() {
        when(repository.findAllByOrganizationId(organizationId, 0, 20)).thenReturn(List.of(userTelephone));

        List<TelephoneOutput> output = new FindAllTelephonesByOrganizationIdImpl(repository)
                .execute(organizationId, 0, 20);

        assertEquals(1, output.size());
        assertEquals(organizationId, output.get(0).organizationId());
    }
}
