package io.chronoflow.auth.controller;

import io.chronoflow.auth.dto.ApiKeyRecordResponse;
import io.chronoflow.auth.dto.RotateApiKeyResponse;
import io.chronoflow.auth.dto.ValidateApiKeyResponse;
import io.chronoflow.auth.service.ApiKeyAuthService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalAuthController {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final ApiKeyAuthService apiKeyAuthService;

    @GetMapping("/api-keys/validate")
    public ValidateApiKeyResponse validateApiKey(
            @RequestHeader(name = API_KEY_HEADER, required = false) String credential
    ) {
        return apiKeyAuthService.validateCredential(credential);
    }

    @GetMapping("/tenants/{tenantId}/api-keys")
    public List<ApiKeyRecordResponse> listTenantApiKeys(@PathVariable("tenantId") UUID tenantId) {
        return apiKeyAuthService.listTenantKeys(tenantId);
    }

    @PostMapping("/tenants/{tenantId}/api-keys/{keyId}/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeApiKey(
            @PathVariable("tenantId") UUID tenantId,
            @PathVariable("keyId") String keyId
    ) {
        apiKeyAuthService.revokeKey(tenantId, keyId);
    }

    @PostMapping("/tenants/{tenantId}/api-keys/{keyId}/rotate")
    public RotateApiKeyResponse rotateApiKey(
            @PathVariable("tenantId") UUID tenantId,
            @PathVariable("keyId") String keyId
    ) {
        return apiKeyAuthService.rotateKey(tenantId, keyId);
    }
}
