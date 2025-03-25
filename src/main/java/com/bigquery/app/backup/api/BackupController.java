package com.bigquery.app.backup.api;

import com.bigquery.app.backup.domain.BackupOrchestrator;
import com.bigquery.app.backup.domain.DatastoreDiscoveryService;
import com.bigquery.app.backup.dto.TableToBackup;
import com.bigquery.app.common.config.BigQueryConfig;
import com.bigquery.app.common.gcs.GcsService;
import com.google.cloud.bigquery.BigQuery;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import static org.springframework.http.ResponseEntity.ok;
import static org.springframework.util.CollectionUtils.isEmpty;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Validated
public class BackupController {

    private final GcsService gcsService;
    private final DatastoreDiscoveryService datastoreDiscoveryService;
    private final BackupOrchestrator backupOrchestrator;
    private final BigQueryConfig bigQueryConfig;

    @PostMapping("/backup")
    public ResponseEntity<String> backupBigQuery(@RequestBody @Valid BackupRequest backupRequest) {
        log.info("Received Backup Request: {}", backupRequest);

        BigQuery bigQueryClient = bigQueryConfig.createBigQueryClient(backupRequest.bigQuery().projectId());
        String gcsUri = backupRequest.backupStorage().gcsOptions().uri();

        // Validate GCS bucket permissions in order to fast fail
        gcsService.validateGcsBucket(gcsUri);

        List<TableToBackup> tablesToBackup = backupRequest.bigQuery().tablesToBackup();

        if (isEmpty(tablesToBackup)) {
            log.info("No tables specified, discovering all datasets in project: {}",
                    backupRequest.bigQuery().projectId());

            tablesToBackup = datastoreDiscoveryService.discoverAllDatasets(bigQueryClient);
            log.info("Discovered {} datasets for backup", tablesToBackup.size());
        }

        for (TableToBackup tableToBackup : tablesToBackup) {
            Set<String> discoveredTables = datastoreDiscoveryService.discoverTables(
                    bigQueryClient,
                    tableToBackup,
                    backupRequest.backupTime()
            );

            backupOrchestrator.exportTables(
                    bigQueryClient,
                    tableToBackup.datasetName(),
                    discoveredTables,
                    backupRequest.backupStorage().gcsOptions(),
                    backupRequest.backupTime()
            );
        }

        return ok("backup has been successfully taken");
    }
}
