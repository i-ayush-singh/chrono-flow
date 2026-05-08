package io.chronoflow.auth.dto;

public record ValidateApiKeyResponse(
        boolean valid,
        String tenantId,
        Integer tenantRateLimitPerMinute
) {
}
