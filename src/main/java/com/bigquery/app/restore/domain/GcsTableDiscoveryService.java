package com.bigquery.app.restore.domain;

import com.bigquery.app.common.gcs.GcsService;
import com.bigquery.app.common.util.JobUtil;
import com.bigquery.app.restore.dto.GcsOptions;
import com.google.cloud.storage.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

import static com.bigquery.app.common.util.GcsPathUtil.buildBackupBasePath;

@Service
@RequiredArgsConstructor
@Slf4j
public class GcsTableDiscoveryService {
    private final GcsService gcsService;

    public Set<String> discoverBackedUpTables(GcsOptions gcsOptions, String datasetName) {
        log.info("Discovering backed up tables for dataset: {}", datasetName);

        String gcsBasePath = buildBackupBasePath(
                gcsOptions.uri(),
                gcsOptions.backupTimestamp(),
                gcsOptions.projectIdOfBackup(),
                datasetName
        );

        log.info("Scanning GCS path: {}", gcsBasePath);

        Bucket bucket = gcsService.getBucket(gcsOptions.uri());

        Pattern tableNamePattern = JobUtil.createTableNamePattern(gcsOptions.importOptions().format());

        return gcsService.findTableNamesFromGcsFiles(
                bucket,
                gcsBasePath,
                tableNamePattern,
                gcsOptions.importOptions().format()
        );
    }
}
