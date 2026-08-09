package com.zera.ms_administrative_core.core.usecase.user.assignManager;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AssignManagerImpl implements AssignManager {

    private final UserRepository userRepository;

    public AssignManagerImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void execute(UUID userId, UUID managerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!(user instanceof Employee employee)) {
            throw new IllegalArgumentException("Only employees can have a manager assigned");
        }

        if (managerId != null) {
            if (managerId.equals(userId)) {
                throw new IllegalArgumentException("An employee cannot be their own manager");
            }
            userRepository.findById(managerId)
                    .orElseThrow(() -> new UserNotFoundException(managerId));
        }

        employee.assignManagerId(managerId);
        userRepository.save(employee);
    }
}
