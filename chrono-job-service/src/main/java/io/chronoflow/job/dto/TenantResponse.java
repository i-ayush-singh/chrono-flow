package io.chronoflow.job.dto;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(UUID id, String name, Instant createdAt) {
}
