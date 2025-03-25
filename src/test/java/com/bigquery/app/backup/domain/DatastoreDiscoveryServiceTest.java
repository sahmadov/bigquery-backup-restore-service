package com.bigquery.app.backup.domain;

import com.bigquery.app.backup.dto.BackupTime;
import com.bigquery.app.backup.dto.TableToBackup;
import com.bigquery.app.common.config.BackupProperties;
import com.bigquery.app.common.exception.ResourceNotFoundException;
import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static com.bigquery.app.util.BigQueryMockUtil.*;
import static com.bigquery.app.util.ConfigTestUtil.setupConfigurationMocks;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.quality.Strictness.LENIENT;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class DatastoreDiscoveryServiceTest {

    private static final String SNAPSHOT_DATASET_NAME = "table_snapshots_dataset";

    @Mock
    private BigQuery bigQuery;

    @Mock
    private BackupProperties backupProperties;

    @Mock
    private BackupProperties.DatasetProperties datasetProperties;

    @Mock
    private BackupProperties.DatasetProperties.SnapshotProperties snapshotProperties;

    @InjectMocks
    private DatastoreDiscoveryService discoveryService;

    @BeforeEach
    public void setUp() {
        when(backupProperties.getDataset()).thenReturn(datasetProperties);
        when(datasetProperties.getSnapshot()).thenReturn(snapshotProperties);
        when(snapshotProperties.getName()).thenReturn(SNAPSHOT_DATASET_NAME);

        setupConfigurationMocks();
    }

    @Test
    public void testIsMatchingTable_WithBothExplicitTableAndFilter() {
        // given
        Table table = createMockTable(bigQuery, "dataset", "myTable");
        Set<String> explicitTables = Set.of("myTable", "otherTable");
        Pattern pattern = discoveryService.createFilterPattern("table_*");

        // when
        boolean result = discoveryService.isMatchingTable(table, pattern, explicitTables);

        // then
        assertTrue(result, "Table should match because it's in explicit list, even though it doesn't match pattern");

        // given
        Table tableMatchesPattern = createMockTable(bigQuery, "dataset", "table_123");

        // when
        boolean resultPattern = discoveryService.isMatchingTable(tableMatchesPattern, pattern, explicitTables);

        // then
        assertTrue(resultPattern, "Table should match because it matches the pattern, even if not in explicit list");
    }

    @Test
    public void testIsMatchingTable_WithNeitherExplicitTableNorFilter() {
        // given
        Table table = createMockTable(bigQuery, "dataset", "unmatched");
        Set<String> explicitTables = Set.of("table1", "table2");
        Pattern pattern = discoveryService.createFilterPattern("prefix_*");

        // when
        boolean result = discoveryService.isMatchingTable(table, pattern, explicitTables);

        // then
        assertFalse(result, "Table should not match as it's neither in explicit list nor matches pattern");
    }

    @Test
    public void testIsMatchingTable_WithOnlyDatasetSpecified_ShouldMatchAllTables() {
        // given
        Table table = createMockTable(bigQuery, "dataset", "any_table");

        Set<String> explicitTables = null;
        Pattern pattern = null;

        // when
        boolean result = discoveryService.isMatchingTable(table, pattern, explicitTables);

        // then
        assertTrue(result, "Table should match when only dataset is specified (no tables or filter)");

        // given
        explicitTables = new HashSet<>();

        // when
        result = discoveryService.isMatchingTable(table, pattern, explicitTables);

        // then
        assertTrue(result, "Table should match when only dataset is specified (empty tables set)");
    }

    @Test
    public void testDiscoverTables_WithTableFilter() {
        // given
        BackupTime backupTime = new BackupTime(LocalDateTime.now().minusDays(1), "UTC");

        Table table1 = createMockTableWithCreationTime(bigQuery, "dataset", "sales_2023", -30);
        Table table2 = createMockTableWithCreationTime(bigQuery, "dataset", "sales_2024", -10);
        Table table3 = createMockTableWithCreationTime(bigQuery, "dataset", "orders", -20);
        Table table4 = createMockTableWithCreationTime(bigQuery, "dataset", "customers", -15);

        TableToBackup tableToBackup = new TableToBackup("dataset", null, "sales_*");
        Page<Table> page = createMockTablePage(table1, table2, table3, table4);

        when(bigQuery.listTables("dataset")).thenReturn(page);

        // when
        Set<String> discovered = discoveryService.discoverTables(bigQuery, tableToBackup, backupTime);

        // then
        assertNotNull(discovered);
        assertEquals(2, discovered.size());
        assertTrue(discovered.contains("sales_2023"));
        assertTrue(discovered.contains("sales_2024"));
        assertFalse(discovered.contains("orders"));
        assertFalse(discovered.contains("customers"));
    }

    @Test
    public void testDiscoverTables_NoTablesOrFilterSpecified() {
        // given
        BackupTime backupTime = new BackupTime(LocalDateTime.now().minusDays(1), "UTC");

        Table standardTable1 = createMockTableWithCreationTime(bigQuery, "dataset", "orders", -30);
        Table standardTable2 = createMockTableWithCreationTime(bigQuery, "dataset", "customers", -10);
        Table viewTable = createMockTableWithType(
                bigQuery, "dataset", "sales_view", -20, TableDefinition.Type.VIEW);
        Table tooNewTable = createMockTableWithCreationTime(bigQuery, "dataset", "new_products", 1);

        TableToBackup tableToBackup = new TableToBackup("dataset", null, null);
        Page<Table> page = createMockTablePage(standardTable1, standardTable2, viewTable, tooNewTable);

        when(bigQuery.listTables("dataset")).thenReturn(page);

        // when
        Set<String> discovered = discoveryService.discoverTables(bigQuery, tableToBackup, backupTime);

        // then
        assertNotNull(discovered);
        assertEquals(2, discovered.size(), "Should discover only standard tables created before backup time");
        assertTrue(discovered.contains("orders"));
        assertTrue(discovered.contains("customers"));
        assertFalse(discovered.contains("sales_view"), "View should be excluded");
        assertFalse(discovered.contains("new_products"), "Too new table should be excluded");
    }

    @Test
    public void testDiscoverTables_MixOfEligibleAndNonEligibleTables() {
        // given
        BackupTime backupTime = new BackupTime(LocalDateTime.now().minusDays(1), "UTC");

        Table eligibleTable1 = createMockTableWithCreationTime(bigQuery, "dataset", "table1", -30);
        Table eligibleTable2 = createMockTableWithCreationTime(bigQuery, "dataset", "table2", -10);
        Table tooNewTable = createMockTableWithCreationTime(bigQuery, "dataset", "table3", 1);
        Table viewTable = createMockTableWithType(
                bigQuery, "dataset", "table4", -20, TableDefinition.Type.VIEW);

        Set<String> explicitTables = new HashSet<>();
        explicitTables.add("table1");
        explicitTables.add("table2");
        explicitTables.add("table3");
        explicitTables.add("table4");

        TableToBackup tableToBackup = new TableToBackup("dataset", explicitTables, null);
        Page<Table> page = createMockTablePage(eligibleTable1, eligibleTable2, tooNewTable, viewTable);

        when(bigQuery.listTables("dataset")).thenReturn(page);

        // when
        Set<String> discovered = discoveryService.discoverTables(bigQuery, tableToBackup, backupTime);

        // then
        assertNotNull(discovered);
        assertEquals(2, discovered.size());
        assertTrue(discovered.contains("table1"));
        assertTrue(discovered.contains("table2"));
        assertFalse(discovered.contains("table3"), "Table created after backup time should be excluded");
        assertFalse(discovered.contains("table4"), "View should be excluded as it's not a standard table");
    }

    @Test
    public void testDiscoverTables_NullBackupTime() {
        // given
        BackupTime nullBackupTime = null;

        Table table1 = createMockTableWithCreationTime(bigQuery, "dataset", "table1", -30);
        Table table2 = createMockTableWithCreationTime(bigQuery, "dataset", "table2", 1);

        Set<String> explicitTables = Set.of("table1", "table2");
        TableToBackup tableToBackup = new TableToBackup("dataset", explicitTables, null);
        Page<Table> page = createMockTablePage(table1, table2);

        when(bigQuery.listTables("dataset")).thenReturn(page);

        // when
        Set<String> discovered = discoveryService.discoverTables(bigQuery, tableToBackup, nullBackupTime);

        // then
        assertNotNull(discovered);
        assertEquals(2, discovered.size(), "All standard tables should be included when backupTime is null");
        assertTrue(discovered.contains("table1"));
        assertTrue(discovered.contains("table2"));
    }

    @Test
    public void testDiscoverAllDatasets_EmptyDatasets() {
        // given
        Page<Dataset> emptyPage = mock(Page.class);
        when(emptyPage.iterateAll()).thenReturn(List.of());

        BigQueryOptions options = mock(BigQueryOptions.class);
        when(options.getProjectId()).thenReturn("project");
        when(bigQuery.getOptions()).thenReturn(options);
        when(bigQuery.listDatasets(eq("project"))).thenReturn(emptyPage);

        // when
        List<TableToBackup> datasets = discoveryService.discoverAllDatasets(bigQuery);

        // then
        assertNotNull(datasets);
        assertTrue(datasets.isEmpty(), "Should return empty list when no datasets found");
    }

    @Test
    public void testDiscoverTables_TablesMatchFilterButAllIneligible() {
        // given
        BackupTime backupTime = new BackupTime(LocalDateTime.now().minusDays(1), "UTC");

        Table viewTable = createMockTableWithType(
                bigQuery, "dataset", "sales_view", -10, TableDefinition.Type.VIEW);
        Table materializedViewTable = createMockTableWithType(
                bigQuery, "dataset", "sales_materialized", -10, TableDefinition.Type.MATERIALIZED_VIEW);
        Table tooNewTable = createMockTableWithCreationTime(bigQuery, "dataset", "sales_recent", 1);

        TableToBackup tableToBackup = new TableToBackup("dataset", null, "sales_*");
        Page<Table> page = createMockTablePage(viewTable, materializedViewTable, tooNewTable);

        when(bigQuery.listTables("dataset")).thenReturn(page);

        // when + then
        ResourceNotFoundException thrown = assertThrows(
                ResourceNotFoundException.class,
                () -> discoveryService.discoverTables(bigQuery, tableToBackup, backupTime)
        );

        assertTrue(thrown.getMessage().contains("dataset"));
        assertTrue(thrown.getMessage().contains("filter: sales_*"));
    }

    @Test
    public void testDiscoverAllDatasets_SkipsSnapshotDataset() {
        // given
        Dataset regularDataset = createMockDataset(bigQuery, "regular_dataset", "US");
        Dataset snapshotDataset = createMockDataset(bigQuery, SNAPSHOT_DATASET_NAME, "US");
        Dataset anotherDataset = createMockDataset(bigQuery, "another_dataset", "US");

        List<Dataset> datasetList = Arrays.asList(regularDataset, snapshotDataset, anotherDataset);
        Page<Dataset> datasetPage = mock(Page.class);
        when(datasetPage.iterateAll()).thenReturn(datasetList);

        BigQueryOptions options = mock(BigQueryOptions.class);
        when(options.getProjectId()).thenReturn("project");
        when(bigQuery.getOptions()).thenReturn(options);
        when(bigQuery.listDatasets(eq("project"))).thenReturn(datasetPage);

        // when
        List<TableToBackup> datasets = discoveryService.discoverAllDatasets(bigQuery);

        // then
        assertEquals(2, datasets.size(), "Should include all datasets except the snapshot dataset");

        List<String> datasetNames = datasets.stream()
                .map(TableToBackup::datasetName)
                .toList();

        assertTrue(datasetNames.contains("regular_dataset"));
        assertTrue(datasetNames.contains("another_dataset"));
        assertFalse(datasetNames.contains(SNAPSHOT_DATASET_NAME), "Snapshot dataset should be skipped");
    }
}