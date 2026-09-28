package com.zera.ms_administrative_core.core.usecase.user.findInvitation;

import com.zera.ms_administrative_core.core.repository.InvitationRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FindPendingInvitationsImpl implements FindPendingInvitations {

    private final InvitationRepository invitationRepository;

    public FindPendingInvitationsImpl(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Override
    public List<PendingInvitationOutput> execute(UUID managerId) {
        return invitationRepository.findAllPendingByManager(managerId, LocalDateTime.now()).stream()
                .map(PendingInvitationOutput::from)
                .toList();
    }
}
