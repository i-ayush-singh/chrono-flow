package io.chronoflow.job.repository;

import io.chronoflow.job.entity.JobDefinition;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobDefinitionRepository extends JpaRepository<JobDefinition, Long> {
    List<JobDefinition> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
