package com.zera.ms_administrative_core.core.usecase.user.countUsersByManager;

import java.util.List;

public interface CountUsersByManager {
    List<ManagerUserCountOutput> execute();
}
