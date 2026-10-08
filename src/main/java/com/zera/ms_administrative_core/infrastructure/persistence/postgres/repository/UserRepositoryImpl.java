package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.ManagerEmployeeCount;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.AuditContextBinder;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.UserMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpa;
    private final UserMapper mapper;
    private final AuditContextBinder auditContext;

    public UserRepositoryImpl(UserJpaRepository jpa,  UserMapper mapper,
            AuditContextBinder auditContext) {
        this.jpa = jpa;
        this.mapper = mapper;
        this.auditContext = auditContext;
    }

    // @Transactional nao e detalhe: a trigger de auditoria le um parametro SET LOCAL, que so
    // alcanca a escrita se as duas coisas acontecerem na MESMA transacao. Sem a transacao, o
    // binding usaria outra conexao do pool e a auditoria registraria autor nulo.
    @Override
    @Transactional
    public void save(User user) {
        auditContext.bindCurrentUser();
        UserJpa entity = mapper.toJpa(user);
        jpa.save(entity);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        auditContext.bindCurrentUser();
        jpa.deleteById(id);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpa.findByEmail(email.value())
                .map(mapper::toDomain);
    }

    // UserRepositoryImpl
    @Override
    public List<User> findAll(Role role, Status status, UUID managerId, UUID unitId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpa.findAllByRoleAndStatusAndManagerId(role, status, managerId, unitId, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public List<ManagerEmployeeCount> countEmployeesGroupedByManager() {
        return jpa.countEmployeesGroupedByManager().stream()
                .map(row -> new ManagerEmployeeCount((UUID) row[0], (Long) row[1]))
                .toList();
    }
}