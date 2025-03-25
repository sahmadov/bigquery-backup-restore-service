package com.bigquery.app.restore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GcsOptions(
        @NotBlank(message = "Valid GCS URI is required (gs://bucket-name)")
        String uri,

        @NotBlank(message = "Backup timestamp is required")
        String backupTimestamp,

        @NotBlank(message = "project Id Of Backup is required")
        String projectIdOfBackup,

        @NotNull(message = "Import options are required")
        ImportOptions importOptions
) {
}