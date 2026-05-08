package io.chronoflow.auth.dto;

public record RotateApiKeyResponse(String keyId, String newKeySecret) {
}
