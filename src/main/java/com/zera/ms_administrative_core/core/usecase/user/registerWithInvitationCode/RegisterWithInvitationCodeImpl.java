package com.zera.ms_administrative_core.core.usecase.user.registerWithInvitationCode;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.UserFactory;
import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.EmailAlreadyInUseException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationExpiredException;
import com.zera.ms_administrative_core.core.domain.exception.InvitationNotFoundException;
import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.InvitationRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;
import com.zera.ms_administrative_core.core.usecase.user.registerUser.RegisterUserOutput;

@Service
public class RegisterWithInvitationCodeImpl implements RegisterWithInvitationCode {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final PasswordHasher hasher;

    public RegisterWithInvitationCodeImpl(InvitationRepository invitationRepository,
            UserRepository userRepository, PasswordHasher hasher) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.hasher = hasher;
    }

    @Override
    public RegisterUserOutput execute(RegisterWithInvitationCodeCommand command) {
        Invitation invitation = invitationRepository.findPendingByCode(command.code())
                .orElseThrow(() -> new InvitationNotFoundException(command.code()));

        if (invitation.isExpired()) {
            throw new InvitationExpiredException(command.code());
        }

        Email email = new Email(command.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyInUseException(email);
        }

        HashedPassword hashedPassword = hasher.hash(new RawPassword(command.rawPassword()));

        User employee = UserFactory.create(
                Role.EMPLOYEE,
                UUID.randomUUID(),
                command.name(),
                email,
                hashedPassword,
                Status.ACTIVE,
                invitation.getUnitId(),
                invitation.getManagerId()
        );

        userRepository.save(employee);

        invitation.markUsed(employee.getUserId());
        invitationRepository.save(invitation);

        return new RegisterUserOutput(
                employee.getUserId(), employee.getName(), employee.getEmail(),
                employee.role(), invitation.getManagerId()
        );
    }
}
