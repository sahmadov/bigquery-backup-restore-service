package com.bigquery.app.backup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GcsOptions(
        @NotBlank
        String uri,

        @Valid
        @NotNull
        ExportOptions exportOptions
) {

}