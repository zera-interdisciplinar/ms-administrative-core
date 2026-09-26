package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.EmployeeJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.ManagerJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A consulta em si nunca rodou contra um banco: {@link UserRepositoryImplTest} mocka o
 * {@link UserJpaRepository} e so confere que os parametros chegam la, sem provar que a JPQL
 * filtra certo. Aqui ela roda contra um H2 real, com {@code TREAT} e o discriminador de fato
 * em jogo.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserJpaRepositoryTest {

    @Autowired
    private UserJpaRepository repository;

    private UserJpa manager(UUID unitId) {
        return new ManagerJpa(UUID.randomUUID(), "Gestor", "gestor-" + UUID.randomUUID() + "@zera.com",
                "hash", Status.ACTIVE, unitId, LocalDateTime.now(), LocalDateTime.now());
    }

    private UserJpa employee(UUID unitId, UUID managerId) {
        return new EmployeeJpa(UUID.randomUUID(), "Operario", "operario-" + UUID.randomUUID() + "@zera.com",
                "hash", Status.ACTIVE, unitId, LocalDateTime.now(), LocalDateTime.now(), managerId);
    }

    @Test
    void shouldFilterByUnitId() {
        UUID unitA = UUID.randomUUID();
        UUID unitB = UUID.randomUUID();
        repository.save(manager(unitA));
        repository.save(manager(unitB));

        Page<UserJpa> result = repository.findAllByRoleAndStatusAndManagerId(
                null, null, null, unitA, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUnitId()).isEqualTo(unitA);
    }

    /** unitId nulo nao restringe: e o comportamento que os outros filtros ja tinham. */
    @Test
    void shouldNotFilterWhenUnitIdIsNull() {
        repository.save(manager(UUID.randomUUID()));
        repository.save(manager(UUID.randomUUID()));

        Page<UserJpa> result = repository.findAllByRoleAndStatusAndManagerId(
                null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(2);
    }

    /**
     * unitId e coluna da classe base (UserJpa), diferente de managerId que so existe em
     * EmployeeJpa por tras de um TREAT. Um MANAGER precisa aparecer no filtro por unidade mesmo
     * sem ser tratado como EmployeeJpa.
     */
    @Test
    void shouldFilterByUnitIdAcrossManagerAndEmployee() {
        UUID unit = UUID.randomUUID();
        UserJpa savedManager = repository.save(manager(unit));
        UserJpa savedEmployee = repository.save(employee(unit, UUID.randomUUID()));
        repository.save(manager(UUID.randomUUID()));

        Page<UserJpa> result = repository.findAllByRoleAndStatusAndManagerId(
                null, null, null, unit, PageRequest.of(0, 10));

        assertThat(result.getContent()).extracting(UserJpa::getId)
                .containsExactlyInAnyOrder(savedManager.getId(), savedEmployee.getId());
    }

    @Test
    void shouldCombineUnitIdWithRoleAndStatus() {
        UUID unit = UUID.randomUUID();
        repository.save(manager(unit));
        repository.save(employee(unit, UUID.randomUUID()));

        Page<UserJpa> result = repository.findAllByRoleAndStatusAndManagerId(
                Role.MANAGER, Status.ACTIVE, null, unit, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    void shouldReturnEmptyForAnUnknownUnit() {
        repository.save(manager(UUID.randomUUID()));

        Page<UserJpa> result = repository.findAllByRoleAndStatusAndManagerId(
                null, null, null, UUID.randomUUID(), PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }
}
