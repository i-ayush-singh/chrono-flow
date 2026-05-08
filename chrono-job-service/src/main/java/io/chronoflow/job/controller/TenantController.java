package io.chronoflow.job.controller;

import io.chronoflow.job.dto.CreateApiKeyResponse;
import io.chronoflow.job.dto.CreateTenantRequest;
import io.chronoflow.job.dto.TenantResponse;
import io.chronoflow.job.service.TenantService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse createTenant(@Valid @RequestBody CreateTenantRequest request) {
        return tenantService.createTenant(request);
    }

    @PostMapping("/{tenantId}/api-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateApiKeyResponse createApiKey(@PathVariable("tenantId") UUID tenantId) {
        return tenantService.createApiKey(tenantId);
    }
}
