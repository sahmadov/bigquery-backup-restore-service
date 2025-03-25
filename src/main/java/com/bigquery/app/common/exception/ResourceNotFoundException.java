package com.bigquery.app.common.exception;

import org.springframework.http.HttpStatus;

public final class ResourceNotFoundException extends BigQueryBackupException {
    public ResourceNotFoundException(String resource, String identifier) {
        super(
                String.format("%s not found with identifier: %s", resource, identifier),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
    }
}