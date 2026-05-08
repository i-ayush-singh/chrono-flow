package io.chronoflow.executor;

import io.chronoflow.executor.config.ExecutorProperties;
import io.chronoflow.executor.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({KafkaTopicsProperties.class, ExecutorProperties.class})
public class ChronoExecutorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoExecutorServiceApplication.class, args);
    }
}
