package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.ManagerJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRepositoryImplTest {

    @Mock
    private UserJpaRepository jpa;

    private final UserMapper mapper = new UserMapper();

    private UserRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new UserRepositoryImpl(jpa, mapper);
    }

    @Test
    @DisplayName("Deve salvar um usuário")
    void shouldSaveUser() {
        User user = createManager();
        
        repository.save(user);

        verify(jpa).save(any(UserJpa.class));
    }

    @Test
    @DisplayName("Deve deletar um usuário")
    void shouldDeleteUser() {
        UUID id = UUID.randomUUID();
        
        repository.delete(id);

        verify(jpa).deleteById(id);
    }

    @Test
    @DisplayName("Deve encontrar por ID")
    void shouldFindById() {
        UUID id = UUID.randomUUID();
        ManagerJpa jpaEntity = createManagerJpa(id);
        when(jpa.findById(id)).thenReturn(Optional.of(jpaEntity));

        Optional<User> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getUserId());
    }

    @Test
    @DisplayName("Deve encontrar por Email")
    void shouldFindByEmail() {
        Email email = new Email("test@test.com");
        ManagerJpa jpaEntity = createManagerJpa(UUID.randomUUID());
        when(jpa.findByEmail(email.value())).thenReturn(Optional.of(jpaEntity));

        Optional<User> result = repository.findByEmail(email);

        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("Deve retornar todos paginados")
    void shouldFindAll() {
        ManagerJpa jpaEntity = createManagerJpa(UUID.randomUUID());
        Page<UserJpa> page = new PageImpl<>(List.of(jpaEntity));
        when(jpa.findAll(any(Pageable.class))).thenReturn(page);

        List<User> result = repository.findAll(0, 10);

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(jpa).findAll(PageRequest.of(0, 10));
    }

    @Test
    @DisplayName("Deve verificar se existe por email")
    void shouldExistByEmail() {
        Email email = new Email("test@test.com");
        when(jpa.existsByEmail(email.value())).thenReturn(true);

        boolean exists = repository.existsByEmail(email);

        assertTrue(exists);
        verify(jpa).existsByEmail(email.value());
    }

    private User createManager() {
        return com.zera.ms_administrative_core.core.domain.UserFactory.load(
                Role.MANAGER, UUID.randomUUID(), "Name", new Email("test@test.com"),
                new HashedPassword("hash"), Status.ACTIVE, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private ManagerJpa createManagerJpa(UUID id) {
        return new ManagerJpa(id, "Name", "test@test.com", "hash",
                Status.ACTIVE, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
    }
}
