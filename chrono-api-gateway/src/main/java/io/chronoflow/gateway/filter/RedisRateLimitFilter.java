package io.chronoflow.gateway.filter;

import io.chronoflow.gateway.config.RateLimitProperties;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RedisRateLimitFilter implements org.springframework.cloud.gateway.filter.GlobalFilter, Ordered {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/actuator") || path.startsWith("/api/v1/health")) {
            return chain.filter(exchange);
        }

        String tenantId = (String) exchange.getAttribute(ApiKeyAuthFilter.ATTR_TENANT_ID);
        Integer tenantLimit = exchange.getAttribute(ApiKeyAuthFilter.ATTR_TENANT_LIMIT);
        if (tenantId == null || tenantId.isBlank()) {
            return chain.filter(exchange);
        }

        long epochWindow = System.currentTimeMillis() / 1000 / rateLimitProperties.windowSeconds();
        String redisKey = "chronoflow:gateway:ratelimit:tenant:" + tenantId + ":" + epochWindow;
        int effectiveLimit = tenantLimit != null && tenantLimit > 0 ? tenantLimit : rateLimitProperties.maxRequests();

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(redisKey, Duration.ofSeconds(rateLimitProperties.windowSeconds()))
                                .thenReturn(count);
                    }
                    return Mono.just(count);
                })
                .flatMap(count -> {
                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit", String.valueOf(effectiveLimit));
                    exchange.getResponse().getHeaders().add("X-RateLimit-Remaining",
                            String.valueOf(Math.max(effectiveLimit - count, 0)));

                    if (count > effectiveLimit) {
                        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                        byte[] body = "{\"status\":\"RATE_LIMITED\",\"message\":\"too many requests\"}"
                                .getBytes(StandardCharsets.UTF_8);
                        var dataBuffer = exchange.getResponse().bufferFactory().wrap(body);
                        exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                        return exchange.getResponse().writeWith(Mono.just(dataBuffer));
                    }
                    return chain.filter(exchange);
                });
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
