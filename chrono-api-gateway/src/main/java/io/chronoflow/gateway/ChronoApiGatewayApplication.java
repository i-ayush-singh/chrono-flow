package io.chronoflow.gateway;

import io.chronoflow.gateway.config.GatewaySecurityProperties;
import io.chronoflow.gateway.config.KafkaAuditProperties;
import io.chronoflow.gateway.config.RateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GatewaySecurityProperties.class, RateLimitProperties.class, KafkaAuditProperties.class})
public class ChronoApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoApiGatewayApplication.class, args);
    }
}
