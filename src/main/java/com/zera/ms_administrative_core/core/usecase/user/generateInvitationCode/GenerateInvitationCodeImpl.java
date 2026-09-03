package com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.InvitationRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class GenerateInvitationCodeImpl implements GenerateInvitationCode {

    private static final long EXPIRATION_HOURS = 168;
    private static final int MAX_ATTEMPTS = 10;

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public GenerateInvitationCodeImpl(InvitationRepository invitationRepository, UserRepository userRepository) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public GenerateInvitationCodeOutput execute(UUID managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new UserNotFoundException(managerId));

        if (!(manager instanceof Manager)) {
            throw new IllegalArgumentException("Only managers can generate invitation codes");
        }

        Invitation invitation = createUniqueInvitation(manager);

        return new GenerateInvitationCodeOutput(
                invitation.getId(),
                invitation.getCode(),
                invitation.getManagerId(),
                invitation.getUnitId(),
                invitation.getExpiresAt()
        );
    }

    private Invitation createUniqueInvitation(User manager) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateCode();

            if (invitationRepository.findPendingByCode(code).isPresent()) {
                continue;
            }

            Invitation invitation = new Invitation(
                    UUID.randomUUID(),
                    code,
                    manager.getUserId(),
                    manager.getUnitId(),
                    LocalDateTime.now().plusHours(EXPIRATION_HOURS)
            );

            try {
                return invitationRepository.save(invitation);
            } catch (DataIntegrityViolationException raceLost) {
                // outra chamada concorrente gerou o mesmo código; tenta novamente
            }
        }

        throw new IllegalStateException("Could not generate a unique invitation code, please try again");
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1_000_000));
    }
}
