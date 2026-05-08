package io.chronoflow.job.dto;

public record ValidateApiKeyResponse(
        boolean valid,
        String tenantId,
        Integer tenantRateLimitPerMinute
) {
}
