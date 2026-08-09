package com.zera.ms_administrative_core.core.usecase.assignManager;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.core.usecase.user.assignManager.AssignManagerImpl;
import com.zera.ms_administrative_core.util.UserTestFactory;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignManagerImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private AssignManagerImpl useCase;

    private Employee employee;
    private Manager manager;

    @BeforeEach
    void setUp() {
        employee = UserTestFactory.createEmployee();
        manager = UserTestFactory.createManager();
    }

    @Test
    @DisplayName("Deve atribuir o gestor ao funcionário")
    void shouldAssignManagerToEmployee() {
        when(repository.findById(employee.getUserId())).thenReturn(Optional.of(employee));
        when(repository.findById(manager.getUserId())).thenReturn(Optional.of(manager));

        useCase.execute(employee.getUserId(), manager.getUserId());

        assertEquals(manager.getUserId(), employee.getManagerId());
        verify(repository).save(employee);
    }

    @Test
    @DisplayName("Deve remover o gestor quando managerId for nulo")
    void shouldUnassignManagerWhenManagerIdIsNull() {
        employee.assignManagerId(manager.getUserId());
        when(repository.findById(employee.getUserId())).thenReturn(Optional.of(employee));

        useCase.execute(employee.getUserId(), null);

        assertNull(employee.getManagerId());
        verify(repository).save(employee);
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando o funcionário não existe")
    void shouldThrowWhenEmployeeNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(id, manager.getUserId()));
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando o gestor informado não existe")
    void shouldThrowWhenManagerNotFound() {
        UUID managerId = UUID.randomUUID();
        when(repository.findById(employee.getUserId())).thenReturn(Optional.of(employee));
        when(repository.findById(managerId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> useCase.execute(employee.getUserId(), managerId));
    }

    @Test
    @DisplayName("Deve rejeitar quando o usuário alvo não é um funcionário")
    void shouldRejectWhenTargetIsNotEmployee() {
        when(repository.findById(manager.getUserId())).thenReturn(Optional.of(manager));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(manager.getUserId(), UUID.randomUUID()));
        assertEquals("Only employees can have a manager assigned", exception.getMessage());
    }

    @Test
    @DisplayName("Deve rejeitar quando o funcionário tenta ser seu próprio gestor")
    void shouldRejectSelfAssignment() {
        when(repository.findById(employee.getUserId())).thenReturn(Optional.of(employee));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(employee.getUserId(), employee.getUserId()));
        assertEquals("An employee cannot be their own manager", exception.getMessage());
    }
}
