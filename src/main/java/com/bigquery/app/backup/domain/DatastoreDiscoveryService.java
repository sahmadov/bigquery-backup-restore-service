package com.bigquery.app.backup.domain;

import com.bigquery.app.backup.dto.BackupTime;
import com.bigquery.app.backup.dto.TableToBackup;
import com.bigquery.app.common.config.BackupProperties;
import com.bigquery.app.common.exception.ResourceNotFoundException;
import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableDefinition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.time.Instant.ofEpochMilli;
import static java.time.ZoneId.of;
import static java.time.ZonedDateTime.ofInstant;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static org.springframework.util.CollectionUtils.isEmpty;
import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatastoreDiscoveryService {

    private final BackupProperties backupProperties;

    public Set<String> discoverTables(BigQuery bigQueryClient,
                                      TableToBackup tableToBackup,
                                      BackupTime backupTime) {

        var datasetName = tableToBackup.datasetName();
        var explicitTableNames = tableToBackup.tables();

        var tableFilterPattern = createFilterPattern(tableToBackup.tableFilter());

        var discoveredTables = findEligibleTables(
                bigQueryClient.listTables(datasetName),
                tableFilterPattern,
                explicitTableNames,
                backupTime
        );

        if (discoveredTables.isEmpty()) {
            var filterInfo = ofNullable(tableToBackup.tableFilter())
                    .filter(filter -> !filter.isBlank())
                    .map(filter -> String.format(" (filter: %s)", filter))
                    .orElse("");

            throw new ResourceNotFoundException("Tables matching criteria in dataset", datasetName + filterInfo);
        }

        log.info("Discovered eligible tables in dataset '{}': {}", datasetName, discoveredTables);
        return discoveredTables;
    }

    public List<TableToBackup> discoverAllDatasets(BigQuery bigQueryClient) {
        var result = new ArrayList<TableToBackup>();

        var datasets = bigQueryClient.listDatasets(
                bigQueryClient.getOptions().getProjectId()
        );

        for (Dataset dataset : datasets.iterateAll()) {
            var datasetName = dataset.getDatasetId().getDataset();

            if (datasetName.equals(backupProperties.getDataset().getSnapshot().getName())) {
                log.info("Skipping dataset: {}", datasetName);
                continue;
            }

            var tableToBackup = new TableToBackup(
                    datasetName,
                    null,
                    "*"
            );
            result.add(tableToBackup);
            log.info("Added dataset for backup: {}", datasetName);
        }

        return result;
    }

    public Pattern createFilterPattern(String tableFilter) {
        if (!hasText(tableFilter)) {
            return null;
        }
        var patternString = tableFilter.replace("*", ".*");
        log.debug("Created filter pattern: {} from filter: {}", patternString, tableFilter);
        return Pattern.compile(patternString);
    }

    private Set<String> findEligibleTables(Page<Table> allTables,
                                           Pattern tableFilterPattern,
                                           Set<String> explicitTableNames,
                                           BackupTime backupTime) {

        return StreamSupport.stream(allTables.iterateAll().spliterator(), false)
                .filter(table -> isMatchingTable(table, tableFilterPattern, explicitTableNames))
                .filter(table -> isEligibleForBackup(table, backupTime))
                .map(table -> table.getTableId().getTable())
                .collect(Collectors.toSet());
    }

    public boolean isMatchingTable(Table table,
                                   Pattern tableFilterPattern,
                                   Set<String> explicitTableNames) {
        var tableName = table.getTableId().getTable();

        if (isNull(tableFilterPattern) && (isEmpty(explicitTableNames))) {
            log.debug("Table {} matches because no filter or explicit tables specified", tableName);
            return true;
        }

        var matchesFilter = ofNullable(tableFilterPattern)
                .map(pattern -> pattern.matcher(tableName).matches())
                .orElse(false);

        var isExplicitlyListed = ofNullable(explicitTableNames)
                .map(tables -> tables.contains(tableName))
                .orElse(false);

        var result = matchesFilter || isExplicitlyListed;
        if (result) {
            log.debug("Table {} matches criteria: matchesFilter={}, isExplicitlyListed={}",
                    tableName, matchesFilter, isExplicitlyListed);
        }
        return result;
    }

    private boolean isEligibleForBackup(Table table, BackupTime backupTime) {
        if (isNull(table)) {
            return false;
        }

        var tableName = table.getTableId().getTable();

        if (!isStandardTable(table)) {
            log.info("Skipping '{}' as it is not a standard table.", tableName);
            return false;
        }

        if (!wasCreatedBeforeBackupTime(table, backupTime)) {
            log.info("Skipping table '{}' as it was created after the backup time ({}).",
                    tableName, backupTime.toUtcZonedDateTime());
            return false;
        }

        return true;
    }

    public boolean isStandardTable(Table table) {
        var result = table.getDefinition().getType() == TableDefinition.Type.TABLE;
        if (!result) {
            log.debug("Table {} is not a standard table, type: {}",
                    table.getTableId().getTable(), table.getDefinition().getType());
        }
        return result;
    }

    private boolean wasCreatedBeforeBackupTime(Table table, BackupTime backupTime) {
        return ofNullable(backupTime)
                .map(time -> {
                    var tableCreationTime = ofInstant(ofEpochMilli(table.getCreationTime()), of("UTC"));
                    return !tableCreationTime.isAfter(time.toUtcZonedDateTime());
                })
                .orElse(true);
    }
}
