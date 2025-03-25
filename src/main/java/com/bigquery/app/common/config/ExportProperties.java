package com.bigquery.app.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "export")
@Data
public class ExportProperties {

    private DefaultProperties defaultProps = new DefaultProperties();

    @Data
    public static class DefaultProperties {
        private String format = "AVRO";
        private boolean overwrite = true;
        private boolean header = true;
        private String compression = "GZIP";
        private String fieldDelimiter = ",";
        private Integer threadPoolSize = 4;
        private Integer threadQueueCapacity = 10;
    }
}