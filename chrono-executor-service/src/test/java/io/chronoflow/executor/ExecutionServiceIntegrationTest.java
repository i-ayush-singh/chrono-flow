package io.chronoflow.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.chronoflow.executor.client.WebhookHttpClient;
import io.chronoflow.executor.entity.ExecutionRecord;
import io.chronoflow.executor.entity.ExecutionStatus;
import io.chronoflow.executor.model.ExecuteEvent;
import io.chronoflow.executor.repository.ExecutionRecordRepository;
import io.chronoflow.executor.service.ExecutionService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class ExecutionServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("chronoflow")
            .withUsername("chronoflow")
            .withPassword("chronoflow");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired
    private ExecutionService executionService;

    @Autowired
    private ExecutionRecordRepository executionRecordRepository;

    @MockBean
    private WebhookHttpClient webhookHttpClient;

    @MockBean
    @SuppressWarnings("rawtypes")
    private KafkaTemplate kafkaTemplate;

    @Test
    void process_success_isIdempotentAndStored() {
        when(webhookHttpClient.post(eq("https://example.com/hook"), anyMap()))
                .thenReturn(new ResponseEntity<>("ok", HttpStatus.OK));

        ExecuteEvent event = new ExecuteEvent(
                "JOB_EXECUTE",
                "exec-1",
                "job-1",
                "tenant-1",
                "https://example.com/hook",
                Instant.now().toString(),
                1
        );

        executionService.process(event);
        executionService.process(event);

        Optional<ExecutionRecord> saved = executionRecordRepository.findById("exec-1");
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        verify(webhookHttpClient, times(1)).post(eq("https://example.com/hook"), anyMap());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
