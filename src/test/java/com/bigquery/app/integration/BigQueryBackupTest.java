package com.bigquery.app.integration;

import com.bigquery.app.backup.domain.BackupOrchestrator;
import com.bigquery.app.backup.domain.DatastoreDiscoveryService;
import com.bigquery.app.common.config.BackupProperties;
import com.bigquery.app.common.config.BigQueryConfig;
import com.bigquery.app.common.gcs.GcsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.QueryJobConfiguration;
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
import java.util.List;
import java.util.Set;

import static com.bigquery.app.util.BigQueryMockUtil.*;
import static com.bigquery.app.util.ConfigTestUtil.setupConfigurationMocks;
import static com.bigquery.app.util.GcsMockUtil.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class BigQueryBackupTest {

    private static final String BUCKET_NAME = "my-backup-bucket";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GcsService gcsService;

    @Autowired
    private DatastoreDiscoveryService datastoreDiscoveryService;

    @MockitoBean
    private BigQuery bigQuery;

    @MockitoBean
    private BigQueryConfig bigQueryConfig;

    @MockitoBean
    private Storage storage;

    @MockitoBean
    private BackupProperties backupProperties;

    @MockitoBean
    private BackupProperties.DatasetProperties datasetProperties;

    @MockitoBean
    private BackupProperties.DatasetProperties.SnapshotProperties snapshotProperties;

    @MockitoSpyBean
    private BackupOrchestrator backupOrchestrator;

    private HttpHeaders headers;

    @BeforeEach
    void setup() throws InterruptedException {
        when(bigQueryConfig.createBigQueryClient(any())).thenReturn(bigQuery);
        when(bigQuery.getOptions()).thenReturn(mock(BigQueryOptions.class));
        when(bigQuery.getOptions().getProjectId()).thenReturn(PROJECT_ID);

        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        mockSuccessfulQueryExecution(bigQuery);

        when(backupProperties.getDataset()).thenReturn(datasetProperties);
        when(datasetProperties.getSnapshot()).thenReturn(snapshotProperties);
        when(snapshotProperties.getName()).thenReturn("table_snapshots_dataset");
        when(snapshotProperties.getPrefix()).thenReturn("_SNAPSHOT_");
        when(snapshotProperties.getExpirationDays()).thenReturn(1L);

        setupConfigurationMocks();
    }

    @Test
    @DisplayName("Test valid backup request with minimal fields (verifying GcsService calls)")
    void testValidMinimalBackupRequest() throws IOException, InterruptedException {
        // given
        String requestJson = readJsonFromFile("requests/backup/backup-request-valid-minimal.json");

        mockDatasetListing(bigQuery, "datasetA", "datasetB");
        mockTablesInDataset(bigQuery, "datasetA", "table_A1", "table_A2");
        mockTablesInDataset(bigQuery, "datasetB", "table_B1", "table_B2");

        Bucket mockBucket = setupMockGcsEnvironment(storage, BUCKET_NAME);

        // when
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/backup", request, String.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("backup has been successfully taken", response.getBody());

        verify(backupOrchestrator).exportTables(
                eq(bigQuery),
                eq("datasetA"),
                eq(Set.of("table_A1", "table_A2")),
                any(),
                any()
        );

        verify(backupOrchestrator).exportTables(
                eq(bigQuery),
                eq("datasetB"),
                eq(Set.of("table_B1", "table_B2")),
                any(),
                any()
        );

        ArgumentCaptor<String> datasetNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(bigQuery, times(2)).listTables(datasetNameCaptor.capture());
        List<String> listedDatasets = datasetNameCaptor.getAllValues();
        assertTrue(listedDatasets.contains("datasetA"));
        assertTrue(listedDatasets.contains("datasetB"));
        verify(bigQuery).listDatasets(eq(PROJECT_ID));

        ArgumentCaptor<QueryJobConfiguration> queryCaptor = createQueryCaptor();
        verify(bigQuery, times(8)).query(queryCaptor.capture());

        ArgumentCaptor<String> bucketNameCaptor = createBucketNameCaptor();
        verify(storage).get(bucketNameCaptor.capture());
        assertEquals(BUCKET_NAME, bucketNameCaptor.getValue());

        ArgumentCaptor<List<String>> permissionsCaptor = createPermissionsCaptor();
        verify(storage).testIamPermissions(eq(BUCKET_NAME), permissionsCaptor.capture());
        List<String> actualPerms = permissionsCaptor.getValue();
        assertTrue(actualPerms.contains("storage.buckets.get"));
        assertTrue(actualPerms.contains("storage.objects.create"));
        assertTrue(actualPerms.contains("storage.objects.list"));
    }

    @Test
    @DisplayName("Test valid backup request with CSV format and multithreading")
    void testValidCsvThreadedBackupRequest() throws IOException, InterruptedException {
        // given
        String requestJson = readJsonFromFile("requests/backup/backup-request-valid-csv-threaded.json");

        createMockDataset(bigQuery, "ecommerce_data", "europe-west3");
        mockTablesInDataset(bigQuery, "ecommerce_data", "orders", "sales_2024_01");

        Bucket mockBucket = setupMockGcsEnvironment(storage, BUCKET_NAME);

        // when
        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/backup", request, String.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("backup has been successfully taken", response.getBody());

        verify(backupOrchestrator).exportTables(
                eq(bigQuery),
                eq("ecommerce_data"),
                eq(Set.of("orders", "sales_2024_01")),
                any(),
                any()
        );

        verify(storage).get(BUCKET_NAME);
        verify(storage).testIamPermissions(eq(BUCKET_NAME), anyList());

        verify(bigQuery, times(4)).query(any(QueryJobConfiguration.class));
    }

    @Test
    @DisplayName("Test valid backup request with explicit tables")
    void testValidExplicitTablesBackupRequest() throws IOException, InterruptedException {
        // given
        String requestJson = readJsonFromFile("requests/backup/backup-request-valid-explicit-tables.json");

        createMockDataset(bigQuery, "analytics_dataset", "europe-west3");
        mockTablesInDataset(bigQuery, "analytics_dataset", "product_metrics", "customer_segmentation");

        Bucket mockBucket = setupMockGcsEnvironment(storage, BUCKET_NAME);

        HttpEntity<String> request = new HttpEntity<>(requestJson, headers);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/backup", request, String.class);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("backup has been successfully taken", response.getBody());

        verify(backupOrchestrator).exportTables(
                eq(bigQuery),
                eq("analytics_dataset"),
                eq(Set.of("product_metrics", "customer_segmentation")),
                any(),
                any()
        );

        verify(storage).get(BUCKET_NAME);
        verify(storage).testIamPermissions(eq(BUCKET_NAME), anyList());

        verify(bigQuery, times(4)).query(any(QueryJobConfiguration.class));
    }

    private String readJsonFromFile(String filePath) throws IOException {
        ClassPathResource resource = new ClassPathResource(filePath);
        Path resPath = resource.getFile().toPath();
        return Files.readString(resPath);
    }
}