package com.zera.ms_administrative_core.core.usecase.findUser;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindAllUsersImpl;
import com.zera.ms_administrative_core.core.usecase.user.findUser.UserOutput;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.util.UserTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllUsersImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private FindAllUsersImpl service;

    private User manager;

    @BeforeEach
    void setUp() {
        manager = UserTestFactory.createManager(); // helper — veja abaixo
    }

    @Test
    @DisplayName("Deve retornar lista de usuários sem filtros")
    void shouldReturnAllUsers() {
        when(repository.findAll(null, null, 0, 10)).thenReturn(List.of(manager));

        List<UserOutput> result = service.execute(null, null, 0, 10);

        assertEquals(1, result.size());
        assertEquals(manager.getUserId(), result.get(0).userId());
        verify(repository).findAll(null, null, 0, 10);
    }

    @Test
    @DisplayName("Deve retornar lista filtrada por role")
    void shouldReturnFilteredByRole() {
        when(repository.findAll(Role.MANAGER, null, 0, 10)).thenReturn(List.of(manager));

        List<UserOutput> result = service.execute(Role.MANAGER, null, 0, 10);

        assertEquals(1, result.size());
        assertEquals(Role.MANAGER, result.get(0).role());
        verify(repository).findAll(Role.MANAGER, null, 0, 10);
    }

    @Test
    @DisplayName("Deve retornar lista filtrada por status")
    void shouldReturnFilteredByStatus() {
        when(repository.findAll(null, Status.ACTIVE, 0, 10)).thenReturn(List.of(manager));

        List<UserOutput> result = service.execute(null, Status.ACTIVE, 0, 10);

        assertEquals(1, result.size());
        assertEquals(Status.ACTIVE, result.get(0).status());
        verify(repository).findAll(null, Status.ACTIVE, 0, 10);
    }

    @Test
    @DisplayName("Deve retornar lista filtrada por role e status")
    void shouldReturnFilteredByRoleAndStatus() {
        when(repository.findAll(Role.MANAGER, Status.ACTIVE, 0, 10)).thenReturn(List.of(manager));

        List<UserOutput> result = service.execute(Role.MANAGER, Status.ACTIVE, 0, 10);

        assertEquals(1, result.size());
        verify(repository).findAll(Role.MANAGER, Status.ACTIVE, 0, 10);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há usuários")
    void shouldReturnEmptyList() {
        when(repository.findAll(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        List<UserOutput> result = service.execute(null, null, 0, 10);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Deve mapear corretamente os campos do UserOutput")
    void shouldMapOutputCorrectly() {
        when(repository.findAll(null, null, 0, 10)).thenReturn(List.of(manager));

        UserOutput output = service.execute(null, null, 0, 10).get(0);

        assertEquals(manager.getUserId(), output.userId());
        assertEquals(manager.getName(), output.name());
        assertEquals(manager.getEmail(), output.email());
        assertEquals(manager.role(), output.role());
        assertEquals(manager.getStatus(), output.status());
        assertEquals(manager.getUnitId(), output.unitId());
    }
}