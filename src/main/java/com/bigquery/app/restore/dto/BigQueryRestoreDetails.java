package com.bigquery.app.restore.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BigQueryRestoreDetails(
        @NotBlank(message = "projectId is mandatory")
        String projectId,

        @NotEmpty(message = "At least one dataset name must be provided")
        @Size(min = 1, message = "At least one dataset name must be provided")
        List<String> datasetsToRestore
) {
}