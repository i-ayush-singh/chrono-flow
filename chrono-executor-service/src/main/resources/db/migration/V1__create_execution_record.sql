CREATE TABLE IF NOT EXISTS execution_record (
    execution_id VARCHAR(64) PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    target_url VARCHAR(1000) NOT NULL,
    triggered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    attempt INTEGER NOT NULL,
    status VARCHAR(24) NOT NULL,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(2000),
    last_status_code INTEGER,
    last_latency_ms BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_execution_record_status_next_attempt
    ON execution_record (status, next_attempt_at);
