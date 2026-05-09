package io.chronoflow.gateway.filter;

import io.chronoflow.gateway.config.GatewaySecurityProperties;
import io.chronoflow.gateway.client.AuthServiceClient;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter implements GlobalFilter, Ordered {

    public static final String ATTR_TENANT_ID = "chronoflow.tenantId";
    public static final String ATTR_TENANT_LIMIT = "chronoflow.tenantLimit";

    private final GatewaySecurityProperties securityProperties;
    private final AuthServiceClient authServiceClient;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();
        if (isPublicPath(path, method)) {
            return chain.filter(exchange);
        }

        String headerName = securityProperties.apiKeyHeader();
        String credential = exchange.getRequest().getHeaders().getFirst(headerName);
        if (credential == null || credential.isBlank()) {
            return unauthorized(exchange);
        }

        return authServiceClient.validate(credential)
                .flatMap(result -> {
                    if (!result.valid()) {
                        return unauthorized(exchange);
                    }
                    exchange.getAttributes().put(ATTR_TENANT_ID, result.tenantId());
                    exchange.getAttributes().put(ATTR_TENANT_LIMIT, result.tenantRateLimitPerMinute());
                    var mutatedRequest = exchange.getRequest().mutate()
                            .header("X-Tenant-Id", result.tenantId())
                            .build();
                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                });
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        if (path.startsWith("/actuator") || path.startsWith("/api/v1/health") || path.startsWith("/demo")) {
            return true;
        }
        if (method == HttpMethod.POST && "/api/v1/tenants".equals(path)) {
            return true;
        }
        return method == HttpMethod.POST && path.matches("^/api/v1/tenants/[^/]+/api-keys$");
    }

    private Mono<Void> unauthorized(org.springframework.web.server.ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        byte[] body = "{\"status\":\"UNAUTHORIZED\",\"message\":\"invalid api key\"}"
                .getBytes(StandardCharsets.UTF_8);
        var dataBuffer = exchange.getResponse().bufferFactory().wrap(body);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
    }

    @Override
    public int getOrder() {
        return -150;
    }
}
