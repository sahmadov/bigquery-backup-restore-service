package com.bigquery.app.restore.api;

import com.bigquery.app.restore.dto.BigQueryRestoreDetails;
import com.bigquery.app.restore.dto.RestoreStorage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RestoreRequest(
        @Valid
        @NotNull(message = "bigQuery details are mandatory")
        BigQueryRestoreDetails bigQuery,

        @Valid
        @NotNull(message = "Restore storage information is required")
        RestoreStorage restoreStorage
) {
}