package io.chronoflow.executor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.chronoflow.executor.client.WebhookHttpClient;
import io.chronoflow.executor.config.ExecutorProperties;
import io.chronoflow.executor.config.KafkaTopicsProperties;
import io.chronoflow.executor.entity.ExecutionRecord;
import io.chronoflow.executor.entity.ExecutionStatus;
import io.chronoflow.executor.model.ExecuteEvent;
import io.chronoflow.executor.repository.ExecutionRecordRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final WebhookHttpClient webhookHttpClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties kafkaTopicsProperties;
    private final ExecutorProperties executorProperties;
    private final ExecutionRecordRepository executionRecordRepository;

    @Transactional
    public void process(ExecuteEvent event) {
        String executionId = event.executionId() == null || event.executionId().isBlank()
                ? event.jobId() + ":" + event.triggeredAt()
                : event.executionId();
        int currentAttempt = event.attempt() == null ? 1 : event.attempt();
        ExecutionRecord record = executionRecordRepository.findById(executionId).orElseGet(() -> {
            ExecutionRecord created = new ExecutionRecord();
            created.setExecutionId(executionId);
            created.setJobId(event.jobId());
            created.setTenantId(event.tenantId());
            created.setTargetUrl(event.targetUrl());
            created.setTriggeredAt(Instant.parse(event.triggeredAt()));
            created.setStatus(ExecutionStatus.PENDING);
            return created;
        });

        if (record.getStatus() == ExecutionStatus.SUCCESS || record.getStatus() == ExecutionStatus.DLQ) {
            log.info("Skipping duplicate execution event executionId={} status={}", executionId, record.getStatus());
            return;
        }

        record.setAttempt(currentAttempt);
        record.setStatus(ExecutionStatus.PENDING);
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
                record.setStatus(ExecutionStatus.SUCCESS);
                record.setLastStatusCode(statusCode);
                record.setLastLatencyMs(latencyMs);
                record.setLastError(null);
                record.setNextAttemptAt(null);
                executionRecordRepository.save(record);
                log.info("Webhook success jobId={} attempt={} status={} latencyMs={}",
                        event.jobId(), currentAttempt, statusCode, latencyMs);
                return;
            }
            throw new RuntimeException("Non-success status code: " + statusCode);
        } catch (Exception ex) {
            handleFailure(record, event, currentAttempt, ex);
        }
    }

    @Scheduled(fixedDelayString = "${app.executor.retry-poll-interval-ms:1000}")
    @Transactional
    public void processPendingRetries() {
        var dueRecords = executionRecordRepository
                .findTop100ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        ExecutionStatus.RETRY_PENDING,
                        Instant.now()
                );
        for (ExecutionRecord record : dueRecords) {
            ExecuteEvent event = new ExecuteEvent(
                    "JOB_EXECUTE_RETRY",
                    record.getExecutionId(),
                    record.getJobId(),
                    record.getTenantId(),
                    record.getTargetUrl(),
                    record.getTriggeredAt().toString(),
                    record.getAttempt() + 1
            );
            process(event);
        }
    }

    private void handleFailure(ExecutionRecord record, ExecuteEvent event, int currentAttempt, Exception ex) {
        record.setLastError(ex.getMessage());
        if (currentAttempt >= executorProperties.maxAttempts()) {
            record.setStatus(ExecutionStatus.DLQ);
            record.setNextAttemptAt(null);
            executionRecordRepository.save(record);
            publishDlq(event, currentAttempt, ex.getMessage());
            log.error("Moved to DLQ jobId={} attempt={} error={}", event.jobId(), currentAttempt, ex.getMessage());
            return;
        }

        int nextAttempt = currentAttempt + 1;
        Instant nextAttemptAt = Instant.now().plus(
                executorProperties.retryBackoffMs() * (long) Math.pow(2, Math.max(0, currentAttempt - 1)),
                ChronoUnit.MILLIS
        );
        record.setStatus(ExecutionStatus.RETRY_PENDING);
        record.setAttempt(currentAttempt);
        record.setNextAttemptAt(nextAttemptAt);
        executionRecordRepository.save(record);
        log.warn("Scheduled retry jobId={} nextAttempt={} reason={}", event.jobId(), nextAttempt, ex.getMessage());
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
