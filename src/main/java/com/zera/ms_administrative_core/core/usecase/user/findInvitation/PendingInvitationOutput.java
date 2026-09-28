package com.zera.ms_administrative_core.core.usecase.user.findInvitation;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;

import java.time.LocalDateTime;

public record PendingInvitationOutput(String code, String inviteeName, LocalDateTime expiresAt) {
    public static PendingInvitationOutput from(Invitation invitation) {
        return new PendingInvitationOutput(
                invitation.getCode(),
                invitation.getInviteeName(),
                invitation.getExpiresAt());
    }
}
