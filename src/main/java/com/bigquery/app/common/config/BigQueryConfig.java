package com.bigquery.app.common.config;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigQueryConfig {

    public BigQuery createBigQueryClient(String sourceProjectId) {
        return BigQueryOptions.newBuilder()
                .setProjectId(sourceProjectId)
                .build()
                .getService();
    }
}
