package io.chronoflow.job.service;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.chronoflow.job.config.KafkaTopicsProperties;
import io.chronoflow.job.dto.CreateJobRequest;
import io.chronoflow.job.dto.JobResponse;
import io.chronoflow.job.entity.JobDefinition;
import io.chronoflow.job.entity.Tenant;
import io.chronoflow.job.exception.BadRequestException;
import io.chronoflow.job.exception.NotFoundException;
import io.chronoflow.job.repository.JobDefinitionRepository;
import io.chronoflow.job.repository.TenantRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JobService {

    private final TenantRepository tenantRepository;
    private final JobDefinitionRepository jobDefinitionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicsProperties kafkaTopicsProperties;

    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + request.tenantId()));
        validateCronExpression(request.cronExpression());

        JobDefinition job = new JobDefinition();
        job.setTenant(tenant);
        job.setName(request.name().trim());
        job.setCronExpression(request.cronExpression().trim());
        job.setTargetUrl(request.targetUrl().trim());
        JobDefinition saved = jobDefinitionRepository.save(job);

        publishJobCreatedEvent(saved);
        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<JobResponse> listJobs(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new NotFoundException("Tenant not found: " + tenantId);
        }
        return jobDefinitionRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::map)
                .toList();
    }

    private void validateCronExpression(String cronExpression) {
        try {
            CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
            parser.parse(cronExpression).validate();
        } catch (Exception e) {
            throw new BadRequestException("Invalid cron expression: " + cronExpression);
        }
    }

    private JobResponse map(JobDefinition job) {
        return new JobResponse(
                job.getId(),
                job.getName(),
                job.getCronExpression(),
                job.getTargetUrl(),
                job.getStatus(),
                job.getCreatedAt()
        );
    }

    private void publishJobCreatedEvent(JobDefinition job) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "eventType", "JOB_CREATED",
                    "jobId", job.getId(),
                    "tenantId", job.getTenant().getId().toString(),
                    "cronExpression", job.getCronExpression(),
                    "targetUrl", job.getTargetUrl()
            ));
            kafkaTemplate.send(kafkaTopicsProperties.jobCreated(), job.getId().toString(), payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize job-created event", ex);
        }
    }
}
