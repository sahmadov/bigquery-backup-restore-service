package com.bigquery.app.backup.domain;

import com.bigquery.app.backup.dto.BackupTime;
import com.bigquery.app.common.bigquery.BigQueryService;
import com.bigquery.app.common.config.BackupProperties;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.DatasetId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.bigquery.app.common.util.TimeUtil.formatTimestampForFileName;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapshotService {
    private final BigQueryService bigQueryService;
    private final BackupProperties backupProperties;

    public DatasetId ensureSnapshotDatasetExists(BigQuery bigQueryClient, String projectId, String location) {
        var snapshotDatasetId = DatasetId.of(projectId,
                backupProperties.getDataset().getSnapshot().getName());

        bigQueryService.ensureDatasetExists(
                bigQueryClient,
                snapshotDatasetId,
                "Dataset containing table snapshots for backup",
                location
        );

        return snapshotDatasetId;
    }

    public String createTableSnapshot(BigQuery bigQueryClient,
                                      String projectId,
                                      String datasetName,
                                      String tableName,
                                      DatasetId snapshotDatasetId,
                                      BackupTime backupTime) {

        var timestampSuffix = formatTimestampForFileName(backupTime.toUtcZonedDateTime());
        var snapshotTableName = getSnapshotTableName(tableName, timestampSuffix);

        log.info("Creating snapshot for table: {}.{}", datasetName, tableName);

        bigQueryService.createSnapshotOfTable(
                bigQueryClient,
                projectId,
                datasetName,
                tableName,
                snapshotDatasetId.getDataset(),
                snapshotTableName,
                backupTime.toFormattedUtcString(),
                backupProperties.getDataset().getSnapshot().getExpirationDays()
        );

        return timestampSuffix;
    }

    public String getSnapshotTableName(String tableName, String timestampSuffix) {
        return String.format("%s%s%s", tableName,
                backupProperties.getDataset().getSnapshot().getPrefix(),
                timestampSuffix);
    }
}