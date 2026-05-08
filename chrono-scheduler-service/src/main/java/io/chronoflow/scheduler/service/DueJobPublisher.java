package io.chronoflow.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.chronoflow.scheduler.config.KafkaTopicsProperties;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DueJobPublisher {

    private final ScheduleIndexService scheduleIndexService;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval-ms:1000}")
    public void publishDueJobs() {
        Instant now = Instant.now();
        Set<String> dueJobIds = scheduleIndexService.getDueJobIds(now);
        if (dueJobIds == null || dueJobIds.isEmpty()) {
            return;
        }

        for (String jobId : dueJobIds) {
            Map<Object, Object> metadata = scheduleIndexService.getJobMetadata(jobId);
            if (metadata.isEmpty()) {
                continue;
            }
            try {
                String cronExpression = String.valueOf(metadata.get("cronExpression"));
                String event = objectMapper.writeValueAsString(Map.of(
                        "eventType", "JOB_EXECUTE",
                        "executionId", UUID.randomUUID().toString(),
                        "jobId", jobId,
                        "tenantId", String.valueOf(metadata.get("tenantId")),
                        "targetUrl", String.valueOf(metadata.get("targetUrl")),
                        "triggeredAt", now.toString()
                ));

                kafkaTemplate.send(kafkaTopicsProperties.jobExecute(), jobId, event);
                scheduleIndexService.reschedule(jobId, cronExpression);
                log.info("Published {} for jobId={}", kafkaTopicsProperties.jobExecute(), jobId);
            } catch (JsonProcessingException ex) {
                log.error("Failed to serialize job execute event for jobId={}", jobId, ex);
            } catch (Exception ex) {
                log.error("Failed while publishing due jobId={}", jobId, ex);
            }
        }
    }
}
