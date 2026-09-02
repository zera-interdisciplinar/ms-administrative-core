package com.zera.ms_administrative_core.infrastructure.http.request;

import java.util.UUID;

public record AssignManagerRequest(
        UUID managerId
) {}
