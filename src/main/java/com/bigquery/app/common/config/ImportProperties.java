package com.bigquery.app.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "import")
@Data
public class ImportProperties {

    private DefaultProperties defaultProps = new DefaultProperties();

    @Data
    public static class DefaultProperties {
        private String format = "AVRO";
        private Boolean header = true;
        private String fieldDelimiter = ",";
        private Boolean ignoreUnknownValues = true;
        private Integer maxBadRecords = 0;
        private String writeDisposition = "WRITE_TRUNCATE";
        private String restoreRegion = "europe-west3";
        private Integer threadPoolSize = 5;
        private Integer threadQueueCapacity = 15;
    }
}