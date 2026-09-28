package com.zera.ms_administrative_core.core.usecase.user.findInvitation;

import java.util.List;
import java.util.UUID;

public interface FindPendingInvitations {
    List<PendingInvitationOutput> execute(UUID managerId);
}
