package com.zera.ms_administrative_core.core.usecase.unit;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.exception.UnitNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.core.usecase.unit.deleteUnit.DeleteUnitImpl;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.FindAllUnitsByOrganizationImpl;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.FindUnitByIdImpl;
import com.zera.ms_administrative_core.core.usecase.unit.findUnit.UnitOutput;
import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnitCommand;
import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnitImpl;
import com.zera.ms_administrative_core.core.usecase.unit.registerUnit.RegisterUnitOutput;
import com.zera.ms_administrative_core.core.usecase.unit.renameUnit.RenameUnitImpl;
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
class UnitUseCasesTest {

    @Mock
    private UnitRepository unitRepository;
    @Mock
    private OrganizationRepository organizationRepository;

    private final UUID unitId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    private Organization organization() {
        return new Organization(organizationId, "Org", new Cnpj("11.222.333/0001-81"),
                Status.ACTIVE, new Email("org@email.com"), Plan.FREE);
    }

    private Unit unit() {
        return new Unit(unitId, "Matriz", organizationId);
    }

    // --- RegisterUnit ---

    @Test
    @DisplayName("RegisterUnit should register a unit for an existing organization")
    void shouldRegister() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(organization()));

        RegisterUnitOutput output = new RegisterUnitImpl(unitRepository, organizationRepository)
                .execute(new RegisterUnitCommand(organizationId, "Matriz"));

        assertEquals("Matriz", output.name());
        assertEquals(organizationId, output.organizationId());
        verify(unitRepository).save(any(Unit.class));
    }

    @Test
    @DisplayName("RegisterUnit should fail when the organization does not exist")
    void shouldFailRegisterWhenOrganizationMissing() {
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.empty());

        assertThrows(OrganizationNotFoundException.class,
                () -> new RegisterUnitImpl(unitRepository, organizationRepository)
                        .execute(new RegisterUnitCommand(organizationId, "Matriz")));
        verify(unitRepository, never()).save(any());
    }

    // --- RenameUnit ---

    @Test
    @DisplayName("RenameUnit should rename an existing unit")
    void shouldRename() {
        Unit unit = unit();
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit));

        new RenameUnitImpl(unitRepository).execute(unitId, "Filial");

        assertEquals("Filial", unit.getName());
        verify(unitRepository).save(unit);
    }

    @Test
    @DisplayName("RenameUnit should fail when the unit does not exist")
    void shouldFailRenameWhenMissing() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class,
                () -> new RenameUnitImpl(unitRepository).execute(unitId, "Filial"));
    }

    // --- DeleteUnit ---

    @Test
    @DisplayName("DeleteUnit should delete an existing unit")
    void shouldDelete() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit()));

        new DeleteUnitImpl(unitRepository).execute(unitId);

        verify(unitRepository).delete(unitId);
    }

    @Test
    @DisplayName("DeleteUnit should fail when the unit does not exist")
    void shouldFailDeleteWhenMissing() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class,
                () -> new DeleteUnitImpl(unitRepository).execute(unitId));
        verify(unitRepository, never()).delete(any(UUID.class));
    }

    // --- FindUnit ---

    @Test
    @DisplayName("FindUnitById should return the mapped output")
    void shouldFindById() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.of(unit()));

        UnitOutput output = new FindUnitByIdImpl(unitRepository).execute(unitId);

        assertEquals(unitId, output.unitId());
        assertEquals("Matriz", output.name());
    }

    @Test
    @DisplayName("FindUnitById should fail when the unit does not exist")
    void shouldFailFindByIdWhenMissing() {
        when(unitRepository.findById(unitId)).thenReturn(Optional.empty());

        assertThrows(UnitNotFoundException.class,
                () -> new FindUnitByIdImpl(unitRepository).execute(unitId));
    }

    @Test
    @DisplayName("FindAllUnitsByOrganization should map every unit returned by the repository")
    void shouldFindAll() {
        when(unitRepository.findAll(organizationId, 0, 20)).thenReturn(List.of(unit()));

        List<UnitOutput> output = new FindAllUnitsByOrganizationImpl(unitRepository)
                .execute(organizationId, 0, 20);

        assertEquals(1, output.size());
        assertEquals(organizationId, output.get(0).organizationId());
    }
}
