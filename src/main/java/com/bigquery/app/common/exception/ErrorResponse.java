package com.bigquery.app.common.exception;

import java.time.ZonedDateTime;

public record ErrorResponse(
        String errorCode,
        String message,
        ZonedDateTime timestamp,
        String path
) {
    public static ErrorResponse from(BigQueryBackupException ex, String path) {
        return new ErrorResponse(
                ex.getErrorCode(),
                ex.getMessage(),
                ZonedDateTime.now(),
                path
        );
    }
}