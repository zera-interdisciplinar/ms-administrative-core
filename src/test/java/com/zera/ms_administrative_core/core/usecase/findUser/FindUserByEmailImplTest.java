package com.zera.ms_administrative_core.core.usecase.findUser;

import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.core.usecase.user.findUser.FindUserByEmailImpl;
import com.zera.ms_administrative_core.core.usecase.user.findUser.UserOutput;
import com.zera.ms_administrative_core.util.UserTestFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUserByEmailImplTest {

    @Mock
    private UserRepository repository;

    @InjectMocks
    private FindUserByEmailImpl service;

    private User manager;

    @BeforeEach
    void setUp() {
        manager = UserTestFactory.createManager();
    }

    @Test
    @DisplayName("Deve retornar usuário quando email encontrado")
    void shouldReturnUserWhenEmailFound() {
        when(repository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));

        UserOutput output = service.execute(manager.getEmail().value());

        assertEquals(manager.getEmail(), output.email());
        verify(repository).findByEmail(manager.getEmail());
    }

    @Test
    @DisplayName("Deve lançar UserNotFoundException quando email não encontrado")
    void shouldThrowWhenEmailNotFound() {
        when(repository.findByEmail(any())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.execute("naoexiste@empresa.com"));
    }

    @Test
    @DisplayName("Deve lançar exceção quando email tem formato inválido")
    void shouldThrowWhenEmailInvalid() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute("email-invalido"));
    }
}