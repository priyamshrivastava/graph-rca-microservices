package com.graphrca.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.graphrca.engine", "com.graphrca.common.config"})
@EnableScheduling
public class RcaEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(RcaEngineApplication.class, args);
    }
}
