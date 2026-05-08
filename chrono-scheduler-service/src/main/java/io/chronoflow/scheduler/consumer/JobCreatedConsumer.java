package io.chronoflow.scheduler.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.chronoflow.scheduler.config.KafkaTopicsProperties;
import io.chronoflow.scheduler.service.ScheduleIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final ScheduleIndexService scheduleIndexService;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    @KafkaListener(topics = "${app.kafka.topics.job-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void onJobCreated(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            Long jobId = node.get("jobId").asLong();
            String tenantId = node.get("tenantId").asText();
            String cronExpression = node.get("cronExpression").asText();
            String targetUrl = node.get("targetUrl").asText();

            scheduleIndexService.upsertJob(jobId, tenantId, cronExpression, targetUrl);
            log.info("Consumed {} event for jobId={}", kafkaTopicsProperties.jobCreated(), jobId);
        } catch (Exception ex) {
            log.error("Failed to process job-created payload={}", payload, ex);
        }
    }
}
