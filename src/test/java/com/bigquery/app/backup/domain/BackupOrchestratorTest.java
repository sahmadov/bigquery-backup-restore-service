package com.bigquery.app.backup.domain;

import com.bigquery.app.backup.dto.BackupTime;
import com.bigquery.app.backup.dto.ExportOptions;
import com.bigquery.app.backup.dto.GcsOptions;
import com.bigquery.app.common.concurrent.ThreadingService;
import com.bigquery.app.common.exception.ServiceException;
import com.bigquery.app.common.util.DatasetUtil;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
class BackupOrchestratorTest {

    @Mock
    private SnapshotService snapshotService;

    @Mock
    private BackupExportService exportService;

    @Mock
    private ThreadingService threadingService;

    @Mock
    private BigQuery bigQueryClient;

    @Mock
    private BigQueryOptions bigQueryOptions;

    @Mock
    private GcsOptions gcsOptions;

    @Mock
    private ExportOptions exportOptions;

    @Mock
    private BackupTime backupTime;

    private BackupOrchestrator backupOrchestrator;

    @BeforeEach
    void setUp() {
        backupOrchestrator = new BackupOrchestrator(snapshotService, exportService, threadingService);

        when(bigQueryClient.getOptions()).thenReturn(bigQueryOptions);
        when(bigQueryOptions.getProjectId()).thenReturn("test-project");
        when(gcsOptions.exportOptions()).thenReturn(exportOptions);
    }

    @Test
    void testExportTablesNonMultiThreaded() {
        // given
        when(exportOptions.isMultiThreaded()).thenReturn(false);

        String datasetName = "test-dataset";
        Set<String> tableNames = new HashSet<>(Arrays.asList("table1", "table2"));
        DatasetId snapshotDatasetId = DatasetId.of("test-project", "snapshot-dataset");

        try (MockedStatic<DatasetUtil> datasetUtilMock = mockStatic(DatasetUtil.class)) {
            datasetUtilMock.when(() ->
                    DatasetUtil.getDatasetLocation(bigQueryClient, DatasetId.of("test-project", datasetName))
            ).thenReturn("US");

            when(snapshotService.ensureSnapshotDatasetExists(bigQueryClient, "test-project", "US"))
                    .thenReturn(snapshotDatasetId);

            when(snapshotService.createTableSnapshot(
                    any(BigQuery.class),
                    eq("test-project"),
                    eq(datasetName),
                    any(String.class),
                    eq(snapshotDatasetId),
                    eq(backupTime)
            )).thenReturn("timestamp");

            when(snapshotService.getSnapshotTableName(any(String.class), eq("timestamp")))
                    .thenReturn("snapshotTable");

            // when
            backupOrchestrator.exportTables(bigQueryClient, datasetName, tableNames, gcsOptions, backupTime);

            // then
            for (String tableName : tableNames) {
                verify(snapshotService).createTableSnapshot(bigQueryClient, "test-project", datasetName,
                        tableName, snapshotDatasetId, backupTime);
                verify(snapshotService).getSnapshotTableName(tableName, "timestamp");
                verify(exportService).exportSnapshotToStorage(bigQueryClient, "test-project",
                        snapshotDatasetId.getDataset(), "snapshotTable",
                        datasetName, tableName, gcsOptions, "timestamp");
            }

            verify(threadingService, never()).submitBackupTask(any());
        }
    }

    @Test
    void testExportTablesMultiThreaded() {
        // given
        when(exportOptions.isMultiThreaded()).thenReturn(true);

        String datasetName = "test-dataset";
        Set<String> tableNames = new HashSet<>(Arrays.asList("table1", "table2"));
        DatasetId snapshotDatasetId = DatasetId.of("test-project", "snapshot-dataset");

        try (MockedStatic<DatasetUtil> datasetUtilMock = mockStatic(DatasetUtil.class)) {
            datasetUtilMock.when(() ->
                    DatasetUtil.getDatasetLocation(bigQueryClient, DatasetId.of("test-project", datasetName))
            ).thenReturn("US");

            when(snapshotService.ensureSnapshotDatasetExists(bigQueryClient, "test-project", "US"))
                    .thenReturn(snapshotDatasetId);

            when(snapshotService.createTableSnapshot(
                    any(BigQuery.class),
                    eq("test-project"),
                    eq(datasetName),
                    any(String.class),
                    eq(snapshotDatasetId),
                    eq(backupTime)
            )).thenReturn("timestamp");

            when(snapshotService.getSnapshotTableName(any(String.class), eq("timestamp")))
                    .thenReturn("snapshotTable");

            when(threadingService.submitBackupTask(any()))
                    .thenAnswer(invocation -> {
                        Runnable task = invocation.getArgument(0);
                        task.run();
                        return CompletableFuture.completedFuture(null);
                    });

            // when
            backupOrchestrator.exportTables(bigQueryClient, datasetName, tableNames, gcsOptions, backupTime);

            // then
            verify(threadingService).configureThreadPoolForExport(exportOptions);
            verify(threadingService, times(tableNames.size())).submitBackupTask(any());

            for (String tableName : tableNames) {
                verify(snapshotService).createTableSnapshot(bigQueryClient, "test-project", datasetName,
                        tableName, snapshotDatasetId, backupTime);
                verify(snapshotService).getSnapshotTableName(tableName, "timestamp");
                verify(exportService).exportSnapshotToStorage(bigQueryClient, "test-project",
                        snapshotDatasetId.getDataset(), "snapshotTable",
                        datasetName, tableName, gcsOptions, "timestamp");
            }
        }
    }

    @Test
    void testExportTablesParallelExceptionHandling() {
        // given
        when(exportOptions.isMultiThreaded()).thenReturn(true);
        String datasetName = "test-dataset";
        Set<String> tableNames = new HashSet<>(List.of("table1"));
        DatasetId snapshotDatasetId = DatasetId.of("test-project", "snapshot-dataset");

        try (MockedStatic<DatasetUtil> datasetUtilMock = mockStatic(DatasetUtil.class)) {
            datasetUtilMock.when(() ->
                    DatasetUtil.getDatasetLocation(bigQueryClient, DatasetId.of("test-project", datasetName))
            ).thenReturn("US");

            when(snapshotService.ensureSnapshotDatasetExists(bigQueryClient, "test-project", "US"))
                    .thenReturn(snapshotDatasetId);

            when(snapshotService.createTableSnapshot(
                    any(BigQuery.class),
                    eq("test-project"),
                    eq(datasetName),
                    any(String.class),
                    eq(snapshotDatasetId),
                    eq(backupTime)
            )).thenReturn("timestamp");

            when(snapshotService.getSnapshotTableName(any(String.class), eq("timestamp")))
                    .thenReturn("snapshotTable");

            when(threadingService.submitBackupTask(any()))
                    .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Task failed")));

            // when + then
            try {
                backupOrchestrator.exportTables(bigQueryClient, datasetName, tableNames, gcsOptions, backupTime);
            } catch (ServiceException ex) {
                assertTrue(ex.getMessage().contains("Error during parallel backup"));
            }
        }
    }
}