package io.chronoflow.executor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.chronoflow.executor.client.WebhookHttpClient;
import io.chronoflow.executor.config.ExecutorProperties;
import io.chronoflow.executor.config.KafkaTopicsProperties;
import io.chronoflow.executor.model.ExecuteEvent;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final WebhookHttpClient webhookHttpClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final ExecutorProperties executorProperties;

    public void process(ExecuteEvent event) {
        int currentAttempt = event.attempt() == null ? 1 : event.attempt();
        long start = System.currentTimeMillis();

        try {
            ResponseEntity<String> response = webhookHttpClient.post(event.targetUrl(), Map.of(
                    "jobId", event.jobId(),
                    "tenantId", event.tenantId(),
                    "triggeredAt", event.triggeredAt(),
                    "attempt", currentAttempt
            ));
            long latencyMs = System.currentTimeMillis() - start;
            int statusCode = response.getStatusCode().value();

            if (statusCode >= 200 && statusCode < 300) {
                log.info("Webhook success jobId={} attempt={} status={} latencyMs={}",
                        event.jobId(), currentAttempt, statusCode, latencyMs);
                return;
            }
            throw new RuntimeException("Non-success status code: " + statusCode);
        } catch (Exception ex) {
            handleFailure(event, currentAttempt, ex);
        }
    }

    private void handleFailure(ExecuteEvent event, int currentAttempt, Exception ex) {
        if (currentAttempt >= executorProperties.maxAttempts()) {
            publishDlq(event, currentAttempt, ex.getMessage());
            log.error("Moved to DLQ jobId={} attempt={} error={}", event.jobId(), currentAttempt, ex.getMessage());
            return;
        }

        int nextAttempt = currentAttempt + 1;
        publishRetry(event, nextAttempt, ex.getMessage());
        log.warn("Scheduled retry jobId={} nextAttempt={} reason={}", event.jobId(), nextAttempt, ex.getMessage());
    }

    private void publishRetry(ExecuteEvent event, int nextAttempt, String reason) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "JOB_EXECUTE_RETRY",
                    "jobId", event.jobId(),
                    "tenantId", event.tenantId(),
                    "targetUrl", event.targetUrl(),
                    "triggeredAt", event.triggeredAt(),
                    "attempt", nextAttempt,
                    "scheduledAt", Instant.now().plusMillis(executorProperties.retryBackoffMs()).toString(),
                    "reason", reason
            ));
            kafkaTemplate.send(kafkaTopicsProperties.jobRetry(), event.jobId(), payload);
        } catch (JsonProcessingException jpe) {
            throw new IllegalStateException("Failed to serialize retry payload", jpe);
        }
    }

    private void publishDlq(ExecuteEvent event, int attempt, String reason) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "JOB_EXECUTE_DLQ",
                    "jobId", event.jobId(),
                    "tenantId", event.tenantId(),
                    "targetUrl", event.targetUrl(),
                    "triggeredAt", event.triggeredAt(),
                    "attempt", attempt,
                    "failedAt", Instant.now().toString(),
                    "reason", reason
            ));
            kafkaTemplate.send(kafkaTopicsProperties.jobDlq(), event.jobId(), payload);
        } catch (JsonProcessingException jpe) {
            throw new IllegalStateException("Failed to serialize dlq payload", jpe);
        }
    }
}
