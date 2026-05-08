package io.chronoflow.common;

import java.time.Instant;

public record BaseApiResponse(String status, String message, Instant timestamp) {

    public static BaseApiResponse ok(String message) {
        return new BaseApiResponse("OK", message, Instant.now());
    }
}
