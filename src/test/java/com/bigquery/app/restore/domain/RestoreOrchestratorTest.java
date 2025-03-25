package com.bigquery.app.restore.domain;

import com.bigquery.app.common.concurrent.ThreadingService;
import com.bigquery.app.common.exception.ServiceException;
import com.bigquery.app.restore.api.RestoreRequest;
import com.bigquery.app.restore.dto.BigQueryRestoreDetails;
import com.bigquery.app.restore.dto.GcsOptions;
import com.bigquery.app.restore.dto.ImportOptions;
import com.bigquery.app.restore.dto.RestoreStorage;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.bigquery.app.util.ConfigTestUtil.setupGcsProperties;
import static com.bigquery.app.util.ConfigTestUtil.setupImportProperties;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class RestoreOrchestratorTest {

    @Mock
    private GcsTableDiscoveryService tableDiscoveryService;

    @Mock
    private DatasetRestoreService datasetRestoreService;

    @Mock
    private TableRestoreService tableRestoreService;

    @Mock
    private ThreadingService threadingService;

    @Mock
    private BigQuery bigQuery;

    @InjectMocks
    private RestoreOrchestrator restoreOrchestrator;

    @BeforeEach
    public void setUp() {
        BigQueryOptions bigQueryOptions = mock(BigQueryOptions.class);
        when(bigQuery.getOptions()).thenReturn(bigQueryOptions);
        when(bigQueryOptions.getProjectId()).thenReturn("test-project");

        setupGcsProperties();
        setupImportProperties();
    }

    private RestoreRequest createRestoreRequest(Integer threadCount, Integer queueCapacity) {
        ImportOptions importOptions = new ImportOptions("europe-west3", "AVRO", Boolean.TRUE, null,
                Boolean.TRUE, 0, "WRITE_TRUNCATE", threadCount, queueCapacity);
        GcsOptions gcsOptions = new GcsOptions("gs://test-bucket", "2021-01-01T00:00:00Z", "backup-project", importOptions);
        RestoreStorage restoreStorage = new RestoreStorage(gcsOptions);
        BigQueryRestoreDetails restoreDetails = new BigQueryRestoreDetails("test-project", List.of("dataset1"));
        return new RestoreRequest(restoreDetails, restoreStorage);
    }

    @Test
    public void testRestoreSingleThreadedSuccess() {
        // given
        RestoreRequest request = createRestoreRequest(1, null);

        when(datasetRestoreService.createDestinationDataset(eq(bigQuery), eq("dataset1"), anyString()))
                .thenReturn("restored_dataset1");

        when(tableDiscoveryService.discoverBackedUpTables(any(), eq("dataset1")))
                .thenReturn(Set.of("table1", "table2"));

        // when
        restoreOrchestrator.restore(bigQuery, request);

        // then
        verify(threadingService, never()).configureThreadPoolForImport(any());
        verify(datasetRestoreService).createDestinationDataset(eq(bigQuery), eq("dataset1"), eq("europe-west3"));
        verify(tableDiscoveryService).discoverBackedUpTables(any(), eq("dataset1"));
        verify(tableRestoreService, times(2)).restoreTable(eq(bigQuery), any(), eq("dataset1"),
                eq("restored_dataset1"), anyString());
    }

    @Test
    public void testRestoreMultiThreadedSuccess() {
        // given
        RestoreRequest request = createRestoreRequest(2, 10);

        when(datasetRestoreService.createDestinationDataset(eq(bigQuery), eq("dataset1"), anyString()))
                .thenReturn("restored_dataset1");

        when(tableDiscoveryService.discoverBackedUpTables(any(), eq("dataset1")))
                .thenReturn(Set.of("table1"));

        CompletableFuture<Void> mockFuture = CompletableFuture.completedFuture(null);
        when(threadingService.submitRestoreTask(any())).thenReturn(mockFuture);

        // when
        restoreOrchestrator.restore(bigQuery, request);

        // then
        verify(threadingService).configureThreadPoolForImport(any());
        verify(threadingService).submitRestoreTask(any());
        verify(datasetRestoreService).createDestinationDataset(eq(bigQuery), eq("dataset1"), eq("europe-west3"));
        verify(tableDiscoveryService).discoverBackedUpTables(any(), eq("dataset1"));
    }

    @Test
    public void testRestoreNoTablesFound() {
        // given
        RestoreRequest request = createRestoreRequest(1, null);

        when(datasetRestoreService.createDestinationDataset(eq(bigQuery), eq("dataset1"), anyString()))
                .thenReturn("restored_dataset1");

        when(tableDiscoveryService.discoverBackedUpTables(any(), eq("dataset1")))
                .thenReturn(Set.of());

        // when
        restoreOrchestrator.restore(bigQuery, request);

        // then
        verify(datasetRestoreService).createDestinationDataset(eq(bigQuery), eq("dataset1"), eq("europe-west3"));
        verify(tableDiscoveryService).discoverBackedUpTables(any(), eq("dataset1"));
        verify(tableRestoreService, never()).restoreTable(any(), any(), any(), any(), any());
    }

    @Test
    public void testRestoreTableThrowsException() {
        // given
        RestoreRequest request = createRestoreRequest(1, null);

        when(datasetRestoreService.createDestinationDataset(eq(bigQuery), eq("dataset1"), anyString()))
                .thenReturn("restored_dataset1");

        when(tableDiscoveryService.discoverBackedUpTables(any(), eq("dataset1")))
                .thenReturn(Set.of("table1"));

        doThrow(new ServiceException("Restore", "Import failed", new Exception("Import failed")))
                .when(tableRestoreService)
                .restoreTable(eq(bigQuery), any(), eq("dataset1"), eq("restored_dataset1"), eq("table1"));

        // when + then
        Exception exception = assertThrows(ServiceException.class, () -> restoreOrchestrator.restore(bigQuery, request));
        assertTrue(exception.getMessage().contains("Import failed"));
    }
}