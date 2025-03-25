package com.bigquery.app.common.bigquery;

import com.bigquery.app.common.exception.ConflictException;
import com.bigquery.app.common.exception.ServiceException;
import com.google.cloud.bigquery.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BigQueryServiceTest {

    @Mock
    private BigQuery mockBigQuery;

    @InjectMocks
    private BigQueryService bigQueryService;

    @Test
    public void testEnsureDatasetExists_CreationFails() {
        // given
        DatasetId datasetId = DatasetId.of("project", "dataset");
        when(mockBigQuery.getDataset(datasetId)).thenReturn(null);

        BigQueryException exception = new BigQueryException(500, "Failed to create dataset");
        when(mockBigQuery.create(any(DatasetInfo.class))).thenThrow(exception);

        // when + then
        BigQueryException thrown = assertThrows(
                BigQueryException.class,
                () -> bigQueryService.ensureDatasetExists(mockBigQuery, datasetId, "desc", "location")
        );
        assertEquals(500, thrown.getCode());
        assertEquals("Failed to create dataset", thrown.getMessage());

        verify(mockBigQuery).getDataset(datasetId);
        verify(mockBigQuery).create(any(DatasetInfo.class));
    }

    @Test
    public void testCreateSnapshotOfTable_ConflictException() throws InterruptedException {
        // given
        BigQueryException conflictException = new BigQueryException(409, "Snapshot already exists");
        when(mockBigQuery.query(any(QueryJobConfiguration.class))).thenThrow(conflictException);

        // when + then
        ConflictException thrown = assertThrows(
                ConflictException.class,
                () -> bigQueryService.createSnapshotOfTable(
                        mockBigQuery, "project", "srcDataset", "srcTable",
                        "destDataset", "snapshotTable", "2021-01-01T00:00:00Z", 1L
                )
        );

        assertTrue(thrown.getMessage().contains("Snapshot"));
        assertTrue(thrown.getMessage().contains("already exists"));

        verify(mockBigQuery).query(any(QueryJobConfiguration.class));
    }

    @Test
    public void testCreateSnapshotOfTable_InterruptedException() throws InterruptedException {
        // given
        InterruptedException interruptedException = new InterruptedException("Operation interrupted");
        when(mockBigQuery.query(any(QueryJobConfiguration.class))).thenThrow(interruptedException);

        // when + then
        ServiceException thrown = assertThrows(
                ServiceException.class,
                () -> bigQueryService.createSnapshotOfTable(
                        mockBigQuery, "project", "srcDataset", "srcTable",
                        "destDataset", "snapshotTable", "2021-01-01T00:00:00Z", 1L
                )
        );

        assertTrue(thrown.getMessage().contains("Snapshot Creation"));
        assertTrue(thrown.getMessage().contains("interrupted"));

        assertTrue(Thread.currentThread().isInterrupted());

        verify(mockBigQuery).query(any(QueryJobConfiguration.class));
    }

    @Test
    public void testExecuteExport_InterruptedException() throws InterruptedException {
        // given
        BigQueryService spyService = spy(bigQueryService);

        InterruptedException interruptedException = new InterruptedException("Export interrupted");
        doThrow(interruptedException).when(spyService).executeQuery(eq(mockBigQuery), anyString());

        // when + then
        ServiceException thrown = assertThrows(
                ServiceException.class,
                () -> spyService.executeExport(
                        mockBigQuery,
                        TableId.of("project", "dataset", "table"),
                        "gs://bucket/path", "CSV", true, ",", "GZIP", true
                )
        );

        assertTrue(thrown.getMessage().contains("BigQuery"));
        assertTrue(thrown.getMessage().contains("interrupted"));

        assertTrue(Thread.currentThread().isInterrupted());

        verify(spyService).executeQuery(eq(mockBigQuery), anyString());
    }

    @Test
    public void testExecuteImport_InterruptedException() throws InterruptedException {
        // given
        Job mockJob = mock(Job.class);
        when(mockBigQuery.create(any(JobInfo.class))).thenReturn(mockJob);
        when(mockJob.waitFor()).thenThrow(new InterruptedException("Import interrupted"));

        // when + then
        ServiceException thrown = assertThrows(
                ServiceException.class,
                () -> bigQueryService.executeImport(
                        mockBigQuery,
                        TableId.of("project", "dataset", "table"),
                        "gs://bucket/path", "AVRO", true, ",", true, 0,
                        JobInfo.WriteDisposition.WRITE_TRUNCATE
                )
        );

        assertTrue(thrown.getMessage().contains("Import interrupted"));

        assertTrue(Thread.currentThread().isInterrupted());

        verify(mockBigQuery).create(any(JobInfo.class));
        verify(mockJob).waitFor();
    }

    @Test
    public void testExecuteImport_ValidationException() {
        // given + when + then
        assertThrows(
                com.bigquery.app.common.exception.ValidationException.class,
                () -> bigQueryService.executeImport(
                        mockBigQuery,
                        TableId.of("project", "dataset", "table"),
                        "gs://bucket/path", "INVALID_FORMAT", true, ",", true, 0,
                        JobInfo.WriteDisposition.WRITE_TRUNCATE
                )
        );

        verify(mockBigQuery, never()).create(any(JobInfo.class));
    }

    @Test
    public void testExecuteQuery_BigQueryException() throws InterruptedException {
        // given
        BigQueryException exception = new BigQueryException(400, "Query syntax error");
        when(mockBigQuery.query(any(QueryJobConfiguration.class))).thenThrow(exception);

        // when + then
        BigQueryException thrown = assertThrows(
                BigQueryException.class,
                () -> bigQueryService.executeQuery(mockBigQuery, "SELECT * FROM invalid.syntax")
        );

        assertEquals(400, thrown.getCode());
        assertEquals("Query syntax error", thrown.getMessage());

        verify(mockBigQuery).query(any(QueryJobConfiguration.class));
    }
}