package io.chronoflow.executor;

import io.chronoflow.executor.config.ExecutorProperties;
import io.chronoflow.executor.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({KafkaTopicsProperties.class, ExecutorProperties.class})
public class ChronoExecutorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoExecutorServiceApplication.class, args);
    }
}
