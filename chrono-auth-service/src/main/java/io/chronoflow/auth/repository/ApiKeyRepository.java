package io.chronoflow.auth.repository;

import io.chronoflow.auth.entity.ApiKey;
import io.chronoflow.auth.entity.ApiKeyStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyIdAndStatus(String keyId, ApiKeyStatus status);
    Optional<ApiKey> findByTenantIdAndKeyId(UUID tenantId, String keyId);
    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
