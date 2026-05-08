package io.chronoflow.job.service;

import io.chronoflow.job.dto.CreateApiKeyResponse;
import io.chronoflow.job.dto.CreateTenantRequest;
import io.chronoflow.job.dto.TenantResponse;
import io.chronoflow.job.entity.ApiKey;
import io.chronoflow.job.entity.Tenant;
import io.chronoflow.job.exception.BadRequestException;
import io.chronoflow.job.exception.NotFoundException;
import io.chronoflow.job.repository.ApiKeyRepository;
import io.chronoflow.job.repository.TenantRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final ApiKeyRepository apiKeyRepository;

    @Transactional
    public TenantResponse createTenant(CreateTenantRequest request) {
        tenantRepository.findByName(request.name()).ifPresent(existing -> {
            throw new BadRequestException("Tenant with same name already exists");
        });
        Tenant tenant = new Tenant();
        tenant.setName(request.name().trim());
        Tenant saved = tenantRepository.save(tenant);
        return new TenantResponse(saved.getId(), saved.getName(), saved.getCreatedAt());
    }

    @Transactional
    public CreateApiKeyResponse createApiKey(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));

        String keyId = "ck_" + UUID.randomUUID().toString().replace("-", "");
        String keySecret = "cs_" + UUID.randomUUID() + UUID.randomUUID().toString().substring(0, 8);

        ApiKey apiKey = new ApiKey();
        apiKey.setTenant(tenant);
        apiKey.setKeyId(keyId);
        apiKey.setKeySecretHash(sha256Hex(keySecret));
        apiKeyRepository.save(apiKey);

        return new CreateApiKeyResponse(keyId, keySecret);
    }

    private String sha256Hex(String plainText) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
