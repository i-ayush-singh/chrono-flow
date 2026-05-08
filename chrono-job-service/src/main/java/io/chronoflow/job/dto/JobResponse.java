package io.chronoflow.job.dto;

import io.chronoflow.job.entity.JobStatus;
import java.time.Instant;

public record JobResponse(
        Long id,
        String name,
        String cronExpression,
        String targetUrl,
        JobStatus status,
        Instant createdAt
) {
}
