package com.zera.ms_administrative_core.core.usecase.user.generateInvitationCode;

import java.util.UUID;

public interface GenerateInvitationCode {
    GenerateInvitationCodeOutput execute(UUID managerId);
}
