package io.chronoflow.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(
        String apiKeyHeader,
        String authServiceBaseUrl,
        String internalAuthPath
) {
}
