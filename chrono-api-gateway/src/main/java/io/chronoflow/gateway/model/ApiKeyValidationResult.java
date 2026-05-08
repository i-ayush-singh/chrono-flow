package io.chronoflow.gateway.model;

public record ApiKeyValidationResult(
        boolean valid,
        String tenantId,
        Integer tenantRateLimitPerMinute
) {
}
