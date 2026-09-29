package com.zera.ms_administrative_core.core.usecase.user.updateUserImage;

import java.util.UUID;

public interface UpdateUserImage {
    void execute(UUID userId, String imageUrl);
}
