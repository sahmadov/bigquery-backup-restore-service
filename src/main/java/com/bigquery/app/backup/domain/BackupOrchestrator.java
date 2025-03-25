package com.bigquery.app.backup.domain;

import com.bigquery.app.backup.dto.BackupTime;
import com.bigquery.app.backup.dto.GcsOptions;
import com.bigquery.app.common.concurrent.ThreadingService;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.DatasetId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.bigquery.app.common.util.DatasetUtil.getDatasetLocation;

@Service
@RequiredArgsConstructor
@Slf4j
public class BackupOrchestrator {
    private final SnapshotService snapshotService;
    private final BackupExportService exportService;
    private final ThreadingService threadingService;

    public void exportTables(BigQuery bigQueryClient,
                             String datasetName,
                             Set<String> tableNames,
                             GcsOptions gcsOptions,
                             BackupTime backupTime) {

        var projectId = bigQueryClient.getOptions().getProjectId();
        var sourceDatasetId = DatasetId.of(projectId, datasetName);
        var location = getDatasetLocation(bigQueryClient, sourceDatasetId);

        var snapshotDatasetId = snapshotService.ensureSnapshotDatasetExists(
                bigQueryClient, projectId, location);

        var useMultiThreading = gcsOptions.exportOptions().isMultiThreaded();

        if (useMultiThreading) {
            threadingService.configureThreadPoolForExport(gcsOptions.exportOptions());
            processTablesInParallel(bigQueryClient, projectId, datasetName, tableNames,
                    snapshotDatasetId, gcsOptions, backupTime);
        } else {
            tableNames.forEach(tableName -> processTable(
                    bigQueryClient,
                    projectId,
                    datasetName,
                    tableName,
                    snapshotDatasetId,
                    gcsOptions,
                    backupTime
            ));
        }
    }

    private void processTablesInParallel(BigQuery bigQueryClient,
                                         String projectId,
                                         String datasetName,
                                         Set<String> tableNames,
                                         DatasetId snapshotDatasetId,
                                         GcsOptions gcsOptions,
                                         BackupTime backupTime) {

        log.info("Processing {} tables in parallel", tableNames.size());
        var futures = new ArrayList<CompletableFuture<Void>>();

        for (String tableName : tableNames) {
            var future = threadingService.submitBackupTask(() -> {
                log.info("Starting backup for table {}.{} in thread: {}",
                        datasetName, tableName, Thread.currentThread().getName());

                processTable(
                        bigQueryClient,
                        projectId,
                        datasetName,
                        tableName,
                        snapshotDatasetId,
                        gcsOptions,
                        backupTime
                );

                log.info("Completed backup for table {}.{}", datasetName, tableName);
            });

            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
            log.info("All parallel backup tasks completed successfully");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new com.bigquery.app.common.exception.ServiceException(
                    "Backup", "Backup process was interrupted", e);
        } catch (ExecutionException e) {
            throw new com.bigquery.app.common.exception.ServiceException(
                    "Backup", "Error during parallel backup: " + e.getCause().getMessage(), e.getCause());
        }
    }

    private void processTable(BigQuery bigQueryClient,
                              String projectId,
                              String datasetName,
                              String tableName,
                              DatasetId snapshotDatasetId,
                              GcsOptions gcsOptions,
                              BackupTime backupTime) {

        var timestampSuffix = snapshotService.createTableSnapshot(
                bigQueryClient, projectId, datasetName, tableName,
                snapshotDatasetId, backupTime);

        exportService.exportSnapshotToStorage(
                bigQueryClient,
                projectId,
                snapshotDatasetId.getDataset(),
                snapshotService.getSnapshotTableName(tableName, timestampSuffix),
                datasetName,
                tableName,
                gcsOptions,
                timestampSuffix
        );
    }
}