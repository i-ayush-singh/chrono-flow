package io.chronoflow.job;

import io.chronoflow.job.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class ChronoJobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChronoJobServiceApplication.class, args);
    }
}
