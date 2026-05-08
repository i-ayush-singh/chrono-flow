package io.chronoflow.executor;

import io.chronoflow.common.BaseApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ExecutorHealthController {

    @GetMapping("/health")
    public BaseApiResponse health() {
        return BaseApiResponse.ok("chrono-executor-service is up");
    }
}
