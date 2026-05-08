package io.chronoflow.job;

import io.chronoflow.common.BaseApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @GetMapping("/health")
    public BaseApiResponse health() {
        return BaseApiResponse.ok("chrono-job-service is up");
    }
}
