package io.chronoflow.job.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CreateJobRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(max = 140) String name,
        @NotBlank @Size(max = 64) String cronExpression,
        @NotBlank @Size(max = 1000) String targetUrl
) {
}
