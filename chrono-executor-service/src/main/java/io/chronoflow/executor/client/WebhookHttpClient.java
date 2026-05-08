package io.chronoflow.executor.client;

import io.chronoflow.executor.config.ExecutorProperties;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class WebhookHttpClient {

    private final ExecutorProperties executorProperties;

    public ResponseEntity<String> post(String targetUrl, Map<String, Object> payload) {
        RestTemplate restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(executorProperties.connectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(executorProperties.readTimeoutMs()))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
        return restTemplate.postForEntity(targetUrl, request, String.class);
    }
}
