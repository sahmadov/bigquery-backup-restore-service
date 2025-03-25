package com.bigquery.app.backup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BackupStorage(
        @Valid
        @NotNull
        GcsOptions gcsOptions
) {}