package com.zera.ms_administrative_core.core.usecase.findUser;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUserByIdImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private FindUserByIdImpl service;

    private User manager;

    @BeforeEach
    void setUp() {
        manager = UserTestFactory.createManager();
    }

    @Test
    @DisplayName("Deve retornar usuário quando encontrado")
    void shouldReturnUserWhenFound() {
        when(repository.findById(manager.getUserId())).thenReturn(Optional.of(manager));

        UserOutput output = service.execute(manager.getUserId());

        assertEquals(manager.getUserId(), output.userId());
        assertEquals(manager.getName(), output.name());
        assertEquals(manager.getEmail(), output.email());
        verify(repository).findById(manager.getUserId());
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando não encontrado")
    void shouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> service.execute(id));
        verify(repository).findById(id);
    }
}