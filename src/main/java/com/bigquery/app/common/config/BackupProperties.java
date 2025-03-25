package com.bigquery.app.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "backup")
@Data
public class BackupProperties {

    private DatasetProperties dataset = new DatasetProperties();

    @Data
    public static class DatasetProperties {
        private SnapshotProperties snapshot = new SnapshotProperties();

        @Data
        public static class SnapshotProperties {
            private String name = "table_snapshots_dataset";
            private String prefix = "_SNAPSHOT_";
            private long expirationDays = 1;
        }
    }
}
