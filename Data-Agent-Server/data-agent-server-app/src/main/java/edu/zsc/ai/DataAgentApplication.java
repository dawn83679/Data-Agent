package edu.zsc.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DataAgent Application Entry Point
 */
@SpringBootApplication
@EnableScheduling
public class DataAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataAgentApplication.class, args);
    }

}

