package com.bigquery.app.common.util;

import com.bigquery.app.common.exception.ResourceNotFoundException;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetId;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static java.util.Optional.ofNullable;

@Component
@Slf4j
@NoArgsConstructor
public final class DatasetUtil {

    public static String getDatasetLocation(BigQuery bigQuery, DatasetId datasetId) {
        return ofNullable(bigQuery.getDataset(datasetId))
                .map(Dataset::getLocation)
                .map(location -> {
                    log.debug("Retrieved location {} for dataset {}", location, datasetId);
                    return location;
                })
                .orElseThrow(() -> {
                    log.error("Dataset not found: {}", datasetId);
                    return new ResourceNotFoundException("Dataset not found: ", datasetId.getDataset());
                });
    }

    public static String createPrefixedDatasetName(String prefix, String datasetName) {
        var result = prefix + datasetName;
        log.debug("Created prefixed dataset name: {} from original: {}", result, datasetName);
        return result;
    }
}