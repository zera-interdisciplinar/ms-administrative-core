package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.UserFactory;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.EmployeeJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.ManagerJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toDomain(UserJpa user) {
        return UserFactory.load(
                user.getRole(),
                user.getId(),
                user.getName(),
                new Email(user.getEmail()),
                new HashedPassword(user.getPassword()),
                user.getStatus(),
                user.getUnitId(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public UserJpa toJpa(User domain) {
        return switch (domain.role()){
            case MANAGER -> new ManagerJpa(
                    domain.getUserId(),
                    domain.getName(),
                    domain.getEmail().value(),
                    domain.getPassword().hash(),
                    domain.getStatus(),
                    domain.getUnitId(),
                    domain.getCreatedAt(),
                    domain.getUpdatedAt()
            );
            case EMPLOYEE -> new EmployeeJpa(
                    domain.getUserId(),
                    domain.getName(),
                    domain.getEmail().value(),
                    domain.getPassword().hash(),
                    domain.getStatus(),
                    domain.getUnitId(),
                    domain.getCreatedAt(),
                    domain.getUpdatedAt()
            );
        };
    }
}
