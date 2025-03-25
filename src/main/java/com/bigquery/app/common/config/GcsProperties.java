package com.bigquery.app.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "gcs")
@Data
public class GcsProperties {

    private BackupProperties backup = new BackupProperties();

    @Data
    public static class BackupProperties {
        private String path = "backups";
    }
}