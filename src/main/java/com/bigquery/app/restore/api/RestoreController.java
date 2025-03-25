package com.bigquery.app.restore.api;

import com.bigquery.app.common.config.BigQueryConfig;
import com.bigquery.app.restore.domain.RestoreOrchestrator;
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

import static org.springframework.http.ResponseEntity.ok;


@RestController
@RequestMapping("/api/v1")
@Validated
@RequiredArgsConstructor
@Slf4j
public class RestoreController {

    private final RestoreOrchestrator restoreOrchestrator;
    private final BigQueryConfig bigQueryConfig;

    @PostMapping("/restore")
    public ResponseEntity<String> restoreTables(@Valid @RequestBody RestoreRequest request) {
        log.info("Received restore request for project: {}", request.bigQuery().projectId());

        BigQuery bigQueryClient = bigQueryConfig.createBigQueryClient(request.bigQuery().projectId());

        restoreOrchestrator.restore(bigQueryClient, request);

        return ok("restore has been successfully processed");
    }
}