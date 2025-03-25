package com.bigquery.app.backup.domain;

import com.bigquery.app.backup.dto.GcsOptions;
import com.bigquery.app.common.bigquery.BigQueryService;
import com.bigquery.app.common.util.GcsPathUtil;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupExportService {
    private final BigQueryService bigQueryService;

    public void exportSnapshotToStorage(BigQuery bigQueryClient,
                                        String projectId,
                                        String snapshotDataset,
                                        String snapshotTableName,
                                        String sourceDatasetName,
                                        String sourceTableName,
                                        GcsOptions gcsOptions,
                                        String timestampSuffix) {

        TableId tableId = TableId.of(projectId, snapshotDataset, snapshotTableName);

        String destinationUri = GcsPathUtil.buildBackupFileUri(
                gcsOptions.uri(),
                timestampSuffix,
                projectId,
                sourceDatasetName,
                sourceTableName,
                gcsOptions.exportOptions().format()
        );

        log.info("Exporting snapshot table {} to GCS: {}", tableId, destinationUri);

        bigQueryService.executeExport(
                bigQueryClient,
                tableId,
                destinationUri,
                gcsOptions.exportOptions().format(),
                gcsOptions.exportOptions().header(),
                gcsOptions.exportOptions().fieldDelimiter(),
                gcsOptions.exportOptions().compression(),
                gcsOptions.exportOptions().overwrite()
        );
    }
}
