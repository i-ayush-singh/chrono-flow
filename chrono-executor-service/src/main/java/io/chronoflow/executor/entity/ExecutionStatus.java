package io.chronoflow.executor.entity;

public enum ExecutionStatus {
    PENDING,
    RETRY_PENDING,
    SUCCESS,
    DLQ
}
