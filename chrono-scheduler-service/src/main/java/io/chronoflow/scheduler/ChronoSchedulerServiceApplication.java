package io.chronoflow.scheduler;

import io.chronoflow.scheduler.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class ChronoSchedulerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoSchedulerServiceApplication.class, args);
    }
}
