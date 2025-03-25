package com.bigquery.app.integration;

import com.bigquery.app.common.config.BigQueryConfig;
import com.bigquery.app.common.config.RestoreProperties;
import com.bigquery.app.common.gcs.GcsService;
import com.bigquery.app.restore.domain.RestoreOrchestrator;
import com.bigquery.app.util.ConfigTestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.*;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.bigquery.app.util.BigQueryMockUtil.createJobInfoCaptor;
import static com.bigquery.app.util.BigQueryMockUtil.mockSuccessfulQueryExecution;
import static com.bigquery.app.util.ConfigTestUtil.*;
import static com.bigquery.app.util.GcsMockUtil.createMockBucket;
import static com.bigquery.app.util.GcsMockUtil.mockBlobListing;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BigQueryRestoreTest {

    private static final String PROJECT_ID = "my-restore-project";
    private static final String BUCKET_NAME = "my-bucket";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GcsService gcsService;

    @MockitoBean
    private BigQuery bigQuery;

    @MockitoBean
    private BigQueryConfig bigQueryConfig;

    @MockitoBean
    private Storage storage;

    @MockitoBean
    private RestoreProperties restoreProperties;

    @MockitoBean
    private RestoreProperties.DatasetProperties datasetProperties;

    @MockitoSpyBean
    private RestoreOrchestrator restoreOrchestrator;

    private HttpHeaders headers;

    @BeforeEach
    void setup() throws InterruptedException {
        when(bigQueryConfig.createBigQueryClient(any())).thenReturn(bigQuery);
        when(bigQuery.getOptions()).thenReturn(mock(BigQueryOptions.class));
        when(bigQuery.getOptions().getProjectId()).thenReturn(PROJECT_ID);

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        mockSuccessfulQueryExecution(bigQuery);

        when(restoreProperties.getDataset()).thenReturn(datasetProperties);
        when(datasetProperties.getPrefix()).thenReturn("restored_");

        setupConfigurationMocks();
    }

    @Test
    @DisplayName("Test valid restore request with CSV format and single threading")
    void testValidCsvSingleThreadedRestoreRequest() throws IOException {
        // given
        String requestJson = readJsonFromFile("requests/restore/valid_csv_single_threaded.json");

        Bucket mockBucket = createMockBucket(BUCKET_NAME);
        when(storage.get(BUCKET_NAME)).thenReturn(mockBucket);

        mockBlobListing(mockBucket,
                "backups/20240101_120000/source-project-123/sales_data/table1-000000000000.csv",
                "backups/20240101_120000/source-project-123/sales_data/table1-000000000001.csv",
                "backups/20240101_120000/source-project-123/sales_data/table1-000000000002.csv",
                "backups/20240101_120000/source-project-123/sales_data/table2-000000000000.csv",
                "backups/20240101_120000/source-project-123/sales_data/table2-000000000001.csv"
        );

        DatasetId restoredDatasetId = DatasetId.of(PROJECT_ID, "restored_sales_data");
        Dataset mockDataset = mock(Dataset.class);
        when(mockDataset.getDatasetId()).thenReturn(restoredDatasetId);
        when(bigQuery.getDataset(any(DatasetId.class))).thenReturn(null).thenReturn(mockDataset);
        when(bigQuery.create(any(DatasetInfo.class))).thenReturn(mockDataset);

        // when
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/restore", request, String.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("restore has been successfully processed", response.getBody());

        verify(restoreOrchestrator).restore(eq(bigQuery), any());

        ArgumentCaptor<DatasetInfo> datasetCaptor = ArgumentCaptor.forClass(DatasetInfo.class);
        verify(bigQuery).create(datasetCaptor.capture());
        assertEquals("restored_sales_data", datasetCaptor.getValue().getDatasetId().getDataset());

        verify(storage).get(BUCKET_NAME);

        ArgumentCaptor<JobInfo> importJobCaptor = createJobInfoCaptor();
        verify(bigQuery, times(2)).create(importJobCaptor.capture());

        List<JobInfo> capturedJobs = importJobCaptor.getAllValues();
        assertTrue(capturedJobs.stream().anyMatch(job -> job.getConfiguration().toString().contains("table1")));
        assertTrue(capturedJobs.stream().anyMatch(job -> job.getConfiguration().toString().contains("table2")));
    }

    @Test
    @DisplayName("Test valid restore request with AVRO format and multi-threading")
    void testValidAvroMultiThreadedRestoreRequest() throws IOException {
        // given
        String requestJson = readJsonFromFile("requests/restore/valid_avro_multi_threaded.json");

        Bucket mockBucket = createMockBucket(BUCKET_NAME);
        when(storage.get(BUCKET_NAME)).thenReturn(mockBucket);

        Map<String, List<String>> datasetBlobs = new HashMap<>();
        datasetBlobs.put("backups/20240101_130000/source-project-456/user_logs", List.of(
                "backups/20240101_130000/source-project-456/user_logs/login-000000000000.avro",
                "backups/20240101_130000/source-project-456/user_logs/session-000000000000.avro"
        ));
        datasetBlobs.put("backups/20240101_130000/source-project-456/purchase_history", List.of(
                "backups/20240101_130000/source-project-456/purchase_history/orders-000000000000.avro",
                "backups/20240101_130000/source-project-456/purchase_history/items-000000000000.avro"
        ));
        mockBlobListing(mockBucket, datasetBlobs);

        DatasetId restoredDatasetUserLogs = DatasetId.of(PROJECT_ID, "restored_user_logs");
        DatasetId restoredDatasetPurchaseHistory = DatasetId.of(PROJECT_ID, "restored_purchase_history");

        Dataset mockDataset1 = mock(Dataset.class);
        Dataset mockDataset2 = mock(Dataset.class);
        when(mockDataset1.getDatasetId()).thenReturn(restoredDatasetUserLogs);
        when(mockDataset2.getDatasetId()).thenReturn(restoredDatasetPurchaseHistory);

        when(bigQuery.getDataset(eq(restoredDatasetUserLogs)))
                .thenReturn(null)
                .thenReturn(mockDataset1);
        when(bigQuery.getDataset(eq(restoredDatasetPurchaseHistory)))
                .thenReturn(null)
                .thenReturn(mockDataset2);

        when(bigQuery.create(any(DatasetInfo.class)))
                .thenReturn(mockDataset1)
                .thenReturn(mockDataset2);

        // when
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/restore", request, String.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("restore has been successfully processed", response.getBody());

        verify(restoreOrchestrator).restore(eq(bigQuery), any());

        ArgumentCaptor<DatasetInfo> datasetCaptor = ArgumentCaptor.forClass(DatasetInfo.class);
        verify(bigQuery, times(2)).create(datasetCaptor.capture());

        List<String> createdDatasets = datasetCaptor.getAllValues().stream()
                .map(datasetInfo -> datasetInfo.getDatasetId().getDataset())
                .toList();

        assertTrue(createdDatasets.contains("restored_user_logs"));
        assertTrue(createdDatasets.contains("restored_purchase_history"));

        verify(storage, times(2)).get(BUCKET_NAME); // One retrieval per dataset

        ArgumentCaptor<JobInfo> importJobCaptor = createJobInfoCaptor();
        verify(bigQuery, times(4)).create(importJobCaptor.capture()); // 4 tables to restore

        List<JobInfo> capturedJobs = importJobCaptor.getAllValues();
        assertTrue(capturedJobs.stream().anyMatch(job -> job.getConfiguration().toString().contains("login")));
        assertTrue(capturedJobs.stream().anyMatch(job -> job.getConfiguration().toString().contains("session")));
        assertTrue(capturedJobs.stream().anyMatch(job -> job.getConfiguration().toString().contains("orders")));
        assertTrue(capturedJobs.stream().anyMatch(job -> job.getConfiguration().toString().contains("items")));
    }

    private String readJsonFromFile(String filePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);
        Path resPath = resource.getFile().toPath();
        return Files.readString(resPath);
    }
}