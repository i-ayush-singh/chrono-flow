package io.chronoflow.gateway.filter;

import io.chronoflow.gateway.config.GatewaySecurityProperties;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    private final GatewaySecurityProperties securityProperties;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator") || path.startsWith("/api/v1/health")) {
            return chain.filter(exchange);
        }

        String headerName = securityProperties.apiKeyHeader();
        String presentedKey = exchange.getRequest().getHeaders().getFirst(headerName);
        if (presentedKey == null || presentedKey.isBlank() || !securityProperties.validApiKeys().contains(presentedKey)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            byte[] body = "{\"status\":\"UNAUTHORIZED\",\"message\":\"invalid api key\"}"
                    .getBytes(StandardCharsets.UTF_8);
            var dataBuffer = exchange.getResponse().bufferFactory().wrap(body);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            return exchange.getResponse().writeWith(Mono.just(dataBuffer));
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
