package com.zera.ms_administrative_core.core.usecase.countUsersByManager;

import com.zera.ms_administrative_core.core.repository.ManagerEmployeeCount;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.core.usecase.user.countUsersByManager.CountUsersByManagerImpl;
import com.zera.ms_administrative_core.core.usecase.user.countUsersByManager.ManagerUserCountOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountUsersByManagerImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private CountUsersByManagerImpl service;

    @Test
    @DisplayName("Deve retornar a contagem de funcionários agrupada por gestor")
    void shouldReturnCountGroupedByManager() {
        UUID managerId = UUID.randomUUID();
        when(repository.countEmployeesGroupedByManager())
                .thenReturn(List.of(new ManagerEmployeeCount(managerId, 4L)));

        List<ManagerUserCountOutput> result = service.execute();

        assertEquals(1, result.size());
        assertEquals(managerId, result.get(0).managerId());
        assertEquals(4L, result.get(0).count());
        verify(repository).countEmployeesGroupedByManager();
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando nenhum funcionário possui gestor")
    void shouldReturnEmptyListWhenNoEmployeesHaveManager() {
        when(repository.countEmployeesGroupedByManager()).thenReturn(List.of());

        List<ManagerUserCountOutput> result = service.execute();

        assertTrue(result.isEmpty());
    }
}
