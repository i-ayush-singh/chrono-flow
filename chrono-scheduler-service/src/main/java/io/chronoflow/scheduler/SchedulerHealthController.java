package io.chronoflow.scheduler;

import io.chronoflow.common.BaseApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class SchedulerHealthController {

    @GetMapping("/health")
    public BaseApiResponse health() {
        return BaseApiResponse.ok("chrono-scheduler-service is up");
    }
}
