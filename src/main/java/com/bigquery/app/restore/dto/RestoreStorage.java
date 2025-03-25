package com.bigquery.app.restore.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record RestoreStorage(
        @Valid
        @NotNull(message = "GCS options are required")
        GcsOptions gcsOptions
) {}
