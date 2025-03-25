package com.bigquery.app.restore.domain;

import com.bigquery.app.common.concurrent.ThreadingService;
import com.bigquery.app.common.exception.ServiceException;
import com.bigquery.app.restore.api.RestoreRequest;
import com.bigquery.app.restore.dto.GcsOptions;
import com.google.cloud.bigquery.BigQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestoreOrchestrator {
    private final GcsTableDiscoveryService tableDiscoveryService;
    private final DatasetRestoreService datasetRestoreService;
    private final TableRestoreService tableRestoreService;
    private final ThreadingService threadingService;

    public void restore(BigQuery bigQueryClient, RestoreRequest request) {
        log.info("Starting restore process");

        var gcsOptions = request.restoreStorage().gcsOptions();
        var datasetsToRestore = request.bigQuery().datasetsToRestore();

        var useMultiThreading = gcsOptions.importOptions().isMultiThreaded();

        if (useMultiThreading) {
            threadingService.configureThreadPoolForImport(gcsOptions.importOptions());
        }

        datasetsToRestore.forEach(datasetName ->
                restoreDataset(bigQueryClient, gcsOptions, datasetName, useMultiThreading)
        );

        log.info("Restore process completed successfully");
    }

    private void restoreDataset(
            BigQuery destinationBigQuery,
            GcsOptions gcsOptions,
            String sourceDatasetName,
            boolean useMultiThreading) {

        log.info("Processing dataset to restore: {}", sourceDatasetName);

        var destinationDatasetName = datasetRestoreService.createDestinationDataset(
                destinationBigQuery,
                sourceDatasetName,
                gcsOptions.importOptions().restoreRegion()
        );

        var tablesToRestore = tableDiscoveryService.discoverBackedUpTables(
                gcsOptions, sourceDatasetName);

        log.info("Found {} tables to restore in dataset {}: {}",
                tablesToRestore.size(), destinationDatasetName, tablesToRestore);

        if (tablesToRestore.isEmpty()) {
            log.warn("No tables found to restore for dataset: {}", sourceDatasetName);
            return;
        }

        if (useMultiThreading) {
            restoreTablesConcurrently(destinationBigQuery, gcsOptions, sourceDatasetName,
                    destinationDatasetName, tablesToRestore);
        } else {
            tablesToRestore.forEach(tableName ->
                    tableRestoreService.restoreTable(
                            destinationBigQuery,
                            gcsOptions,
                            sourceDatasetName,
                            destinationDatasetName,
                            tableName
                    )
            );
        }

        log.info("Dataset {} successfully restored to {}", sourceDatasetName, destinationDatasetName);
    }

    private void restoreTablesConcurrently(
            BigQuery bigQuery,
            GcsOptions gcsOptions,
            String sourceDatasetName,
            String destinationDatasetName,
            java.util.Set<String> tablesToRestore) {

        log.info("Restoring {} tables concurrently for dataset {}",
                tablesToRestore.size(), sourceDatasetName);

        var tableFutures = new ArrayList<CompletableFuture<Void>>();

        for (String tableName : tablesToRestore) {
            var future = threadingService.submitRestoreTask(() -> {
                log.info("Starting restore for table {}.{} in thread: {}",
                        sourceDatasetName, tableName, Thread.currentThread().getName());

                tableRestoreService.restoreTable(
                        bigQuery,
                        gcsOptions,
                        sourceDatasetName,
                        destinationDatasetName,
                        tableName
                );

                log.info("Completed restore for table {}.{}", sourceDatasetName, tableName);
            });

            tableFutures.add(future);
        }

        try {
            CompletableFuture.allOf(tableFutures.toArray(new CompletableFuture[0])).get();
            log.info("All parallel table restore tasks completed successfully for dataset {}", sourceDatasetName);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Restore", "Table restore process was interrupted", e);
        } catch (ExecutionException e) {
            throw new ServiceException("Restore", "Error during parallel table restore: " + e.getCause().getMessage(), e.getCause());
        }
    }
}