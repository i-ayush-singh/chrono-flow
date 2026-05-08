package io.chronoflow.executor.model;

public record ExecuteEvent(
        String eventType,
        String executionId,
        String jobId,
        String tenantId,
        String targetUrl,
        String triggeredAt,
        Integer attempt
) {
}
