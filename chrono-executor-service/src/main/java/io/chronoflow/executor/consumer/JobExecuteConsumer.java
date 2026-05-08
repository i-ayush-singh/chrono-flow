package io.chronoflow.executor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.chronoflow.executor.model.ExecuteEvent;
import io.chronoflow.executor.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecuteConsumer {

    private final ObjectMapper objectMapper;
    private final ExecutionService executionService;

    @KafkaListener(topics = "${app.kafka.topics.job-execute}", groupId = "${spring.kafka.consumer.group-id}")
    public void onJobExecute(String payload) {
        consume(payload);
    }

    private void consume(String payload) {
        try {
            ExecuteEvent event = objectMapper.readValue(payload, ExecuteEvent.class);
            executionService.process(event);
        } catch (Exception ex) {
            log.error("Failed to process execution payload={}", payload, ex);
        }
    }
}
