package com.bigquery.app.common.config;

import com.bigquery.app.backup.dto.ExportOptions;
import com.bigquery.app.common.util.GcsPathUtil;
import com.bigquery.app.restore.dto.ImportOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        BackupProperties.class,
        RestoreProperties.class,
        ThreadPoolProperties.class,
        ExportProperties.class,
        ImportProperties.class,
        GcsProperties.class
})
@RequiredArgsConstructor
@Slf4j
public class ConfigurationInitializer {

    private final BackupProperties backupProperties;
    private final RestoreProperties restoreProperties;
    private final ExportProperties exportProperties;
    private final ImportProperties importProperties;
    private final GcsProperties gcsProperties;

    @PostConstruct
    public void initializeConfigurations() {
        log.info("Initializing application configurations...");

        ExportOptions.setDefaults(exportProperties);
        log.info("Export defaults initialized: format={}, compression={}, threadPoolSize={}",
                exportProperties.getDefaultProps().getFormat(),
                exportProperties.getDefaultProps().getCompression(),
                exportProperties.getDefaultProps().getThreadPoolSize());

        ImportOptions.setDefaults(importProperties);
        log.info("Import defaults initialized: format={}, restoreRegion={}, threadPoolSize={}",
                importProperties.getDefaultProps().getFormat(),
                importProperties.getDefaultProps().getRestoreRegion(),
                importProperties.getDefaultProps().getThreadPoolSize());

        GcsPathUtil.setBackupPath(gcsProperties);
        log.info("GCS backup path set to: {}", gcsProperties.getBackup().getPath());

        log.info("Configuration initialization complete");
    }
}
