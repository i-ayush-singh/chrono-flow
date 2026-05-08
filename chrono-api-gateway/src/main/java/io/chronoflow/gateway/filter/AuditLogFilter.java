package io.chronoflow.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.chronoflow.gateway.config.KafkaAuditProperties;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogFilter implements GlobalFilter, Ordered {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaAuditProperties kafkaAuditProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(org.springframework.web.server.ServerWebExchange exchange,
                             org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        long start = System.currentTimeMillis();
        return chain.filter(exchange).doFinally(signal -> {
            try {
                String tenantId = (String) exchange.getAttribute(ApiKeyAuthFilter.ATTR_TENANT_ID);
                String requestId = exchange.getRequest().getHeaders().getFirst(RequestIdFilter.REQUEST_ID_HEADER);
                int status = exchange.getResponse().getStatusCode() != null
                        ? exchange.getResponse().getStatusCode().value() : 0;
                String payload = objectMapper.writeValueAsString(Map.of(
                        "eventType", "GATEWAY_AUDIT",
                        "timestamp", Instant.now().toString(),
                        "requestId", requestId == null ? "n/a" : requestId,
                        "tenantId", tenantId == null ? "anonymous" : tenantId,
                        "method", exchange.getRequest().getMethod() != null ? exchange.getRequest().getMethod().name() : "UNKNOWN",
                        "path", exchange.getRequest().getURI().getPath(),
                        "status", status,
                        "latencyMs", System.currentTimeMillis() - start
                ));
                kafkaTemplate.send(kafkaAuditProperties.gatewayAudit(), tenantId == null ? "anonymous" : tenantId, payload);
            } catch (JsonProcessingException ex) {
                log.warn("Failed to serialize gateway audit log", ex);
            } catch (Exception ex) {
                log.warn("Failed to publish gateway audit log", ex);
            }
        });
    }

    @Override
    public int getOrder() {
        return -10;
    }
}
