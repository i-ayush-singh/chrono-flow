package io.chronoflow.auth.dto;

import io.chronoflow.auth.entity.ApiKeyStatus;
import java.time.Instant;

public record ApiKeyRecordResponse(
        String keyId,
        ApiKeyStatus status,
        Instant createdAt
) {
}
