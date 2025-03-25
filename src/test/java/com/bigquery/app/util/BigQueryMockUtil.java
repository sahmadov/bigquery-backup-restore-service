package com.bigquery.app.util;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.*;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BigQueryMockUtil {

    public static final String PROJECT_ID = "mock-project-id";

    public static List<Dataset> mockDatasetListing(BigQuery bigQuery, String... datasetNames) {
        Page<Dataset> mockDatasetsPage = mock(Page.class);
        List<Dataset> datasetList = new ArrayList<>();

        setupBigQueryOptions(bigQuery);

        for (String datasetName : datasetNames) {
            Dataset mockDataset = createMockDataset(bigQuery, datasetName, "europe-west3");
            datasetList.add(mockDataset);
        }

        when(mockDatasetsPage.iterateAll()).thenReturn(datasetList);
        when(bigQuery.listDatasets(anyString())).thenReturn(mockDatasetsPage);

        return datasetList;
    }

    private static BigQueryOptions setupBigQueryOptions(BigQuery bigQuery) {
        BigQueryOptions options = mock(BigQueryOptions.class);
        when(options.getProjectId()).thenReturn(PROJECT_ID);
        when(bigQuery.getOptions()).thenReturn(options);
        return options;
    }

    public static Dataset createMockDataset(BigQuery bigQuery, String datasetName, String location) {
        BigQueryOptions options = setupBigQueryOptions(bigQuery);

        Dataset dataset = mock(Dataset.class);
        DatasetId datasetId = DatasetId.of(options.getProjectId(), datasetName);
        when(dataset.getDatasetId()).thenReturn(datasetId);
        when(dataset.getLocation()).thenReturn(location);
        when(bigQuery.getDataset(datasetId)).thenReturn(dataset);
        return dataset;
    }

    public static List<Table> mockTablesInDataset(BigQuery bigQuery, String datasetName, String... tableNames) {
        List<Table> tables = new ArrayList<>();
        for (String tableName : tableNames) {
            tables.add(createMockTable(bigQuery, datasetName, tableName));
        }

        Page<Table> tablePage = mock(Page.class);
        when(tablePage.iterateAll()).thenReturn(tables);
        when(bigQuery.listTables(datasetName)).thenReturn(tablePage);

        return tables;
    }

    public static Table createMockTable(BigQuery bigQuery, String datasetName, String tableName) {
        BigQueryOptions options = setupBigQueryOptions(bigQuery);

        String projectId = options.getProjectId();
        Table table = mock(Table.class);
        TableId tableId = TableId.of(projectId, datasetName, tableName);
        when(table.getTableId()).thenReturn(tableId);

        when(table.getCreationTime()).thenReturn(Instant.now().minus(365, ChronoUnit.DAYS).toEpochMilli());

        StandardTableDefinition tableDefinition = mock(StandardTableDefinition.class);
        when(tableDefinition.getType()).thenReturn(TableDefinition.Type.TABLE);
        when(table.getDefinition()).thenReturn(tableDefinition);

        when(bigQuery.getTable(tableId)).thenReturn(table);
        return table;
    }

    public static Table createMockTableWithCreationTime(BigQuery bigQuery, String datasetName, String tableName, int daysAgo) {
        Table table = createMockTable(bigQuery, datasetName, tableName);
        long creationTime = ZonedDateTime.now(ZoneId.of("UTC")).plusDays(daysAgo).toInstant().toEpochMilli();
        when(table.getCreationTime()).thenReturn(creationTime);
        return table;
    }

    public static Table createMockTableWithType(BigQuery bigQuery, String datasetName, String tableName, int daysAgo, TableDefinition.Type type) {
        BigQueryOptions options = setupBigQueryOptions(bigQuery);

        String projectId = options.getProjectId();
        Table table = mock(Table.class);
        TableId tableId = TableId.of(projectId, datasetName, tableName);
        when(table.getTableId()).thenReturn(tableId);

        long creationTime = ZonedDateTime.now(ZoneId.of("UTC")).plusDays(daysAgo).toInstant().toEpochMilli();
        when(table.getCreationTime()).thenReturn(creationTime);

        TableDefinition tableDefinition = mock(TableDefinition.class);
        when(tableDefinition.getType()).thenReturn(type);
        when(table.getDefinition()).thenReturn(tableDefinition);

        when(bigQuery.getTable(tableId)).thenReturn(table);
        return table;
    }

    public static Page<Table> createMockTablePage(Table... tables) {
        Page<Table> page = mock(Page.class);
        when(page.iterateAll()).thenReturn(Arrays.asList(tables));
        return page;
    }

    public static void mockSuccessfulQueryExecution(BigQuery bigQuery) throws InterruptedException {
        setupBigQueryOptions(bigQuery);

        TableResult tableResult = mock(TableResult.class);
        when(bigQuery.query(any(QueryJobConfiguration.class))).thenReturn(tableResult);

        Job job = mock(Job.class);
        JobStatus jobStatus = mock(JobStatus.class);

        when(bigQuery.create(any(JobInfo.class))).thenReturn(job);
        when(job.waitFor()).thenReturn(job);
        when(job.isDone()).thenReturn(true);
        when(job.getStatus()).thenReturn(jobStatus);
        when(jobStatus.getError()).thenReturn(null);
    }

    public static ArgumentCaptor<QueryJobConfiguration> createQueryCaptor() {
        return ArgumentCaptor.forClass(QueryJobConfiguration.class);
    }

    public static ArgumentCaptor<JobInfo> createJobInfoCaptor() {
        return ArgumentCaptor.forClass(JobInfo.class);
    }
}