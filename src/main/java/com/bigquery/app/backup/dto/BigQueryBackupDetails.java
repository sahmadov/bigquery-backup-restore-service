package com.bigquery.app.backup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BigQueryBackupDetails(
        @NotBlank(message = "projectId is mandatory")
        String projectId,

        @Valid
        List<TableToBackup> tablesToBackup
) {}