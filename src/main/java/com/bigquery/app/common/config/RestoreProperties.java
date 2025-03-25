package com.bigquery.app.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "restore")
@Data
public class RestoreProperties {

    private DatasetProperties dataset = new DatasetProperties();

    @Data
    public static class DatasetProperties {
        private String prefix = "restored_";
    }
}