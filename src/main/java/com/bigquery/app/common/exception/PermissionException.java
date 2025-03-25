package com.bigquery.app.common.exception;

import org.springframework.http.HttpStatus;

public final class PermissionException extends BigQueryBackupException {
    public PermissionException(String resource, String permission) {
        super(
                String.format("Permission denied: %s for resource %s", permission, resource),
                HttpStatus.FORBIDDEN,
                "PERMISSION_DENIED"
        );
    }
}