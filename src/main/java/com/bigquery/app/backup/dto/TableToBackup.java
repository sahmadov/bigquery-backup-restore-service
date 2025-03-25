package com.bigquery.app.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TableToBackup(
        @NotBlank(message = "datasetName is mandatory")
        String datasetName,

        Set<String> tables,

        String tableFilter
) {
}
