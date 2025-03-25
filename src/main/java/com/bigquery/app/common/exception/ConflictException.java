package com.bigquery.app.common.exception;

import org.springframework.http.HttpStatus;

public final class ConflictException extends BigQueryBackupException {
    public ConflictException(String resource, String identifier) {
        super(
                String.format("%s with identifier %s already exists", resource, identifier),
                HttpStatus.CONFLICT,
                "RESOURCE_CONFLICT"
        );
    }
}
