package com.bigquery.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.bigquery.app.common.config")
public class BackupApplication {
    public static void main(String[] args) {
        SpringApplication.run(BackupApplication.class, args);
    }
}
