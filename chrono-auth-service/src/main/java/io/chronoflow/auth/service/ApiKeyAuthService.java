package io.chronoflow.auth.service;

import io.chronoflow.auth.dto.ApiKeyRecordResponse;
import io.chronoflow.auth.dto.RotateApiKeyResponse;
import io.chronoflow.auth.dto.ValidateApiKeyResponse;
import io.chronoflow.auth.entity.ApiKey;
import io.chronoflow.auth.entity.ApiKeyStatus;
import io.chronoflow.auth.repository.ApiKeyRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ApiKeyAuthService {

    private final ApiKeyRepository apiKeyRepository;

    @Transactional(readOnly = true)
    public ValidateApiKeyResponse validateCredential(String rawCredential) {
        if (rawCredential == null || rawCredential.isBlank() || !rawCredential.contains(":")) {
            return new ValidateApiKeyResponse(false, null, null);
        }
        String[] parts = rawCredential.split(":", 2);
        String keyId = parts[0];
        String keySecret = parts[1];
        if (keyId.isBlank() || keySecret.isBlank()) {
            return new ValidateApiKeyResponse(false, null, null);
        }

        return apiKeyRepository.findByKeyIdAndStatus(keyId, ApiKeyStatus.ACTIVE)
                .filter(apiKey -> apiKey.getKeySecretHash().equals(sha256Hex(keySecret)))
                .map(apiKey -> new ValidateApiKeyResponse(
                        true,
                        apiKey.getTenant().getId().toString(),
                        apiKey.getTenant().getRateLimitPerMinute()
                ))
                .orElse(new ValidateApiKeyResponse(false, null, null));
    }

    @Transactional(readOnly = true)
    public List<ApiKeyRecordResponse> listTenantKeys(UUID tenantId) {
        return apiKeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(key -> new ApiKeyRecordResponse(key.getKeyId(), key.getStatus(), key.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void revokeKey(UUID tenantId, String keyId) {
        ApiKey key = apiKeyRepository.findByTenantIdAndKeyId(tenantId, keyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Api key not found"));
        key.setStatus(ApiKeyStatus.REVOKED);
        apiKeyRepository.save(key);
    }

    @Transactional
    public RotateApiKeyResponse rotateKey(UUID tenantId, String keyId) {
        ApiKey key = apiKeyRepository.findByTenantIdAndKeyId(tenantId, keyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Api key not found"));
        String newSecret = "cs_" + UUID.randomUUID() + UUID.randomUUID().toString().substring(0, 8);
        key.setKeySecretHash(sha256Hex(newSecret));
        key.setStatus(ApiKeyStatus.ACTIVE);
        apiKeyRepository.save(key);
        return new RotateApiKeyResponse(key.getKeyId(), newSecret);
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
