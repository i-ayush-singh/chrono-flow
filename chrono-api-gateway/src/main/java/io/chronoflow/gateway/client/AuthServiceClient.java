package io.chronoflow.gateway.client;

import io.chronoflow.gateway.config.GatewaySecurityProperties;
import io.chronoflow.gateway.model.ApiKeyValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final GatewaySecurityProperties securityProperties;
    private final WebClient webClient = WebClient.builder().build();

    public Mono<ApiKeyValidationResult> validate(String rawCredential) {
        return webClient.get()
                .uri(securityProperties.authServiceBaseUrl() + securityProperties.internalAuthPath())
                .header(securityProperties.apiKeyHeader(), rawCredential)
                .retrieve()
                .bodyToMono(ApiKeyValidationResult.class)
                .onErrorReturn(new ApiKeyValidationResult(false, null, null));
    }
}
