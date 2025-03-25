package com.bigquery.app.restore.domain;

import com.bigquery.app.common.bigquery.BigQueryService;
import com.bigquery.app.common.exception.ServiceException;
import com.bigquery.app.restore.dto.GcsOptions;
import com.bigquery.app.restore.dto.ImportOptions;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.TableId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.bigquery.app.common.util.GcsPathUtil.buildBackupFileUri;
import static com.bigquery.app.common.util.JobUtil.resolveWriteDisposition;

@Service
@RequiredArgsConstructor
@Slf4j
public class TableRestoreService {
    private final BigQueryService bigQueryService;

    public void restoreTable(
            BigQuery bigQuery,
            GcsOptions gcsOptions,
            String sourceDatasetName,
            String destinationDatasetName,
            String tableName) {

        log.info("Restoring table: {}.{} to {}.{}",
                sourceDatasetName, tableName, destinationDatasetName, tableName);

        String sourceUri = buildBackupFileUri(
                gcsOptions.uri(),
                gcsOptions.backupTimestamp(),
                gcsOptions.projectIdOfBackup(),
                sourceDatasetName,
                tableName,
                gcsOptions.importOptions().format()
        );

        log.info("Source URI for restore: {}", sourceUri);

        TableId destinationTableId = TableId.of(destinationDatasetName, tableName);

        JobInfo.WriteDisposition writeDisposition = resolveWriteDisposition(
                gcsOptions.importOptions().writeDisposition());

        try {
            importTableFromGcs(
                    bigQuery,
                    destinationTableId,
                    sourceUri,
                    gcsOptions.importOptions(),
                    writeDisposition
            );
            log.info("Table {} successfully restored to {}.{}",
                    tableName, destinationDatasetName, tableName);
        } catch (Exception e) {
            log.error("Error restoring table {}: {}", tableName, e.getMessage());
            throw new ServiceException("Restore", "Failed to restore table: " + tableName, e);
        }
    }

    private void importTableFromGcs(
            BigQuery bigQuery,
            TableId tableId,
            String sourceUri,
            ImportOptions importOptions,
            JobInfo.WriteDisposition writeDisposition) {

        bigQueryService.executeImport(
                bigQuery,
                tableId,
                sourceUri,
                importOptions.format(),
                importOptions.header(),
                importOptions.fieldDelimiter(),
                importOptions.ignoreUnknownValues(),
                importOptions.maxBadRecords(),
                writeDisposition
        );
    }
}
