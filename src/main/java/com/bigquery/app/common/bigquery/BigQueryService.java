package com.bigquery.app.common.bigquery;

import com.bigquery.app.common.exception.ConflictException;
import com.bigquery.app.common.exception.ServiceException;
import com.bigquery.app.common.exception.ValidationException;
import com.google.cloud.bigquery.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class BigQueryService {

    public Dataset ensureDatasetExists(BigQuery bigQuery, DatasetId datasetId, String description, String location) {
        var dataset = bigQuery.getDataset(datasetId);

        if (nonNull(dataset)) {
            log.info("Dataset already exists: {}", datasetId.getDataset());
            return dataset;
        }

        log.info("Creating dataset: {}", datasetId.getDataset());
        var datasetInfo = DatasetInfo.newBuilder(datasetId)
                .setDescription(description)
                .setLocation(location)
                .build();

        var createdDataset = bigQuery.create(datasetInfo);
        log.info("Created dataset: {}", datasetId.getDataset());
        return createdDataset;
    }

    public void createSnapshotOfTable(
            BigQuery bigQuery,
            String projectId,
            String sourceDataset,
            String sourceTable,
            String destinationDataset,
            String snapshotTable,
            String snapshotTime,
            long expirationDays
    ) {
        log.info("Creating snapshot for table {}.{} at time {}", sourceDataset, sourceTable, snapshotTime);

        var query = buildSnapshotQuery(
                projectId,
                destinationDataset,
                snapshotTable,
                projectId,
                sourceDataset,
                sourceTable,
                snapshotTime,
                expirationDays
        );

        try {
            executeQuery(bigQuery, query);
            log.info("Created snapshot: {}.{}", destinationDataset, snapshotTable);
        } catch (BigQueryException e) {
            if (e.getCode() == 409) {
                var snapshotId = String.format("%s.%s", destinationDataset, snapshotTable);
                log.warn("Snapshot already exists: {}", snapshotId);
                throw new ConflictException("Snapshot", snapshotId);
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("Snapshot Creation", "Snapshot creation was interrupted", e);
        }
    }

    public void executeExport(
            BigQuery bigQuery,
            TableId sourceTable,
            String destinationUri,
            String format,
            Boolean header,
            String fieldDelimiter,
            String compression,
            Boolean overwrite
    ) {
        log.info("Exporting table {} to GCS: {}", sourceTable, destinationUri);

        var optionsBuilder = new StringBuilder()
                .append(String.format("uri='%s'", destinationUri))
                .append(String.format(", format='%s'", format))
                .append(String.format(", overwrite=%s", overwrite));

        if ("CSV".equalsIgnoreCase(format)) {
            optionsBuilder.append(String.format(", header=%s", header))
                    .append(String.format(", field_delimiter='%s'", fieldDelimiter))
                    .append(String.format(", compression='%s'", compression));
        }

        var exportQuery = """
                EXPORT DATA OPTIONS (%s) AS SELECT * FROM %s.%s.%s
                """.formatted(
                optionsBuilder,
                sourceTable.getProject(),
                sourceTable.getDataset(),
                sourceTable.getTable()
        );

        log.info("Export query:\n{}", exportQuery);

        try {
            executeQuery(bigQuery, exportQuery);
            log.info("Successfully exported table {} to GCS.", sourceTable);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("BigQuery", "Export operation was interrupted", e);
        }
    }

    public void executeImport(
            BigQuery bigQuery,
            TableId destinationTable,
            String sourceUri,
            String format,
            boolean header,
            String fieldDelimiter,
            boolean ignoreUnknownValues,
            int maxBadRecords,
            JobInfo.WriteDisposition writeDisposition
    ) {
        log.info("Importing from GCS: {} to table: {}", sourceUri, destinationTable);

        var configBuilder = LoadJobConfiguration.newBuilder(destinationTable, sourceUri)
                .setAutodetect(true);

        switch (format.toUpperCase()) {
            case "CSV" -> configBuilder.setFormatOptions(CsvOptions.newBuilder()
                    .setSkipLeadingRows(header ? 1 : 0)
                    .setFieldDelimiter(fieldDelimiter)
                    .build());
            case "JSON" -> configBuilder.setFormatOptions(FormatOptions.json());
            case "AVRO" ->  {
                configBuilder.setFormatOptions(FormatOptions.avro());
                configBuilder.setUseAvroLogicalTypes(true);
            }
            case "PARQUET" -> configBuilder.setFormatOptions(FormatOptions.parquet());
            default -> throw new ValidationException("Unsupported format: " + format);
        }

        configBuilder.setWriteDisposition(writeDisposition)
                .setIgnoreUnknownValues(ignoreUnknownValues)
                .setMaxBadRecords(maxBadRecords);

        var loadConfig = configBuilder.build();
        log.info("Load job configuration: {}", loadConfig);

        try {
            var loadJob = bigQuery.create(JobInfo.of(loadConfig));
            loadJob = loadJob.waitFor();

            if (loadJob.isDone() && isNull(loadJob.getStatus().getError())) {
                log.info("Table {} successfully imported from {}", destinationTable, sourceUri);
            } else {
                var status = loadJob.getStatus();
                var errorMsg = nonNull(status.getError()) ? status.getError().toString() : "Unknown error";
                log.error("Error loading table {}: {}", destinationTable, errorMsg);
                throw new ServiceException("BigQuery", "Failed to import to table: " + destinationTable,
                        new Exception(errorMsg));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Import interrupted for table {}", destinationTable, e);
            throw new ServiceException("BigQuery", "Import interrupted for table: " + destinationTable, e);
        }
    }

    public TableResult executeQuery(BigQuery bigQuery, String query) throws InterruptedException {
        var queryConfig = QueryJobConfiguration.newBuilder(query).build();
        return bigQuery.query(queryConfig);
    }

    private String buildSnapshotQuery(
            String projectId,
            String backupDatasetName,
            String snapshotTableName,
            String sourceProjectId,
            String sourceDataset,
            String sourceTableName,
            String backupTimeString,
            long expirationDays
    ) {
        return """
                CREATE SNAPSHOT TABLE %s.%s.%s
                CLONE %s.%s.%s
                FOR SYSTEM_TIME AS OF TIMESTAMP('%s')
                OPTIONS(expiration_timestamp = TIMESTAMP_ADD(CURRENT_TIMESTAMP(), INTERVAL %d DAY))
                """.formatted(
                projectId, backupDatasetName, snapshotTableName,
                sourceProjectId, sourceDataset, sourceTableName,
                backupTimeString, expirationDays);
    }
}
