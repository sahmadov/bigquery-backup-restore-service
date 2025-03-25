package com.bigquery.app.common.exception;

import org.springframework.http.HttpStatus;

public final class ValidationException extends BigQueryBackupException {
    public ValidationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }
}