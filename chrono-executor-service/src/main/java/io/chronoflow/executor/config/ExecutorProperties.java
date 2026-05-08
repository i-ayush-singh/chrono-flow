package io.chronoflow.executor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.executor")
public record ExecutorProperties(int maxAttempts, long retryBackoffMs, int connectTimeoutMs, int readTimeoutMs) {
}
