package com.bigquery.app.backup.api;

import com.bigquery.app.backup.dto.BackupStorage;
import com.bigquery.app.backup.dto.BackupTime;
import com.bigquery.app.backup.dto.BigQueryBackupDetails;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

import static java.util.Objects.isNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BackupRequest(
        @NotNull(message = "bigQuery details are mandatory")
        @Valid
        BigQueryBackupDetails bigQuery,

        @NotNull(message = "backupStorage is mandatory")
        @Valid
        BackupStorage backupStorage,

        @Valid
        BackupTime backupTime
) {
    public BackupRequest {
        if (isNull(backupTime)) {
            backupTime = new BackupTime(LocalDateTime.now(), "UTC");
        }
    }

    public BackupRequest(BigQueryBackupDetails bigQuery, BackupStorage backupStorage) {
        this(bigQuery, backupStorage, new BackupTime(LocalDateTime.now(), "UTC"));
    }
}
