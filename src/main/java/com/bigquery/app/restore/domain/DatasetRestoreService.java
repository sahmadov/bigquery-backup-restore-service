package com.bigquery.app.restore.domain;

import com.bigquery.app.common.bigquery.BigQueryService;
import com.bigquery.app.common.config.RestoreProperties;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.bigquery.app.common.util.DatasetUtil.createPrefixedDatasetName;

@Service
@RequiredArgsConstructor
@Slf4j
public class DatasetRestoreService {
    private final BigQueryService bigQueryService;
    private final RestoreProperties restoreProperties;

    public String createDestinationDataset(
            BigQuery bigQuery,
            String sourceDatasetName,
            String location) {

        String destinationDatasetName = createPrefixedDatasetName(
                restoreProperties.getDataset().getPrefix(), sourceDatasetName);

        log.info("Ensuring destination dataset exists: {}", destinationDatasetName);

        String projectId = bigQuery.getOptions().getProjectId();
        DatasetId datasetId = DatasetId.of(projectId, destinationDatasetName);

        Dataset dataset = bigQueryService.ensureDatasetExists(
                bigQuery,
                datasetId,
                "Restored dataset from backup",
                location
        );

        return dataset.getDatasetId().getDataset();
    }
}
