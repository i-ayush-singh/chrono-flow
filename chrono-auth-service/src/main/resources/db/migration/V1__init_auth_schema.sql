CREATE TABLE IF NOT EXISTS tenant (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    rate_limit_per_minute INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS api_key (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant (id),
    key_id VARCHAR(64) NOT NULL UNIQUE,
    key_secret_hash VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_api_key_tenant_id ON api_key (tenant_id);
