package io.chronoflow.executor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "execution_record")
public class ExecutionRecord {

    @Id
    @Column(name = "execution_id", nullable = false, updatable = false, length = 64)
    private String executionId;

    @Column(name = "job_id", nullable = false, length = 64)
    private String jobId;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "target_url", nullable = false, length = 1000)
    private String targetUrl;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(name = "attempt", nullable = false)
    private Integer attempt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ExecutionStatus status;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "last_status_code")
    private Integer lastStatusCode;

    @Column(name = "last_latency_ms")
    private Long lastLatencyMs;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (attempt == null) {
            attempt = 0;
        }
        if (status == null) {
            status = ExecutionStatus.PENDING;
        }
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
