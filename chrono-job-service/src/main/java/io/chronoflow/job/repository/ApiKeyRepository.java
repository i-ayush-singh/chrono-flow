package io.chronoflow.job.repository;

import io.chronoflow.job.entity.ApiKey;
import io.chronoflow.job.entity.ApiKeyStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyId(String keyId);
    Optional<ApiKey> findByKeyIdAndStatus(String keyId, ApiKeyStatus status);
}
