package com.bigquery.app.common.exception;

import org.springframework.http.HttpStatus;

public final class ServiceException extends BigQueryBackupException {
    public ServiceException(String service, String message) {
        super(
                String.format("%s service error: %s", service, message),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SERVICE_ERROR"
        );
    }

    public ServiceException(String service, String message, Throwable cause) {
        super(
                String.format("%s service error: %s", service, message),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "SERVICE_ERROR"
        );
        initCause(cause);
    }
}