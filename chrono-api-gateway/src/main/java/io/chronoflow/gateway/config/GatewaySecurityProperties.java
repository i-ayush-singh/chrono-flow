package io.chronoflow.gateway.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public record GatewaySecurityProperties(
        String apiKeyHeader,
        List<String> validApiKeys
) {
}
