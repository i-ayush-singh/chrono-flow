package io.chronoflow.job.controller;

import io.chronoflow.job.dto.ValidateApiKeyResponse;
import io.chronoflow.job.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class InternalAuthController {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final TenantService tenantService;

    @GetMapping("/api-keys/validate")
    public ValidateApiKeyResponse validateApiKey(
            @RequestHeader(name = API_KEY_HEADER, required = false) String credential,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String ignoredAuthorization
    ) {
        return tenantService.validateApiKey(credential);
    }
}
