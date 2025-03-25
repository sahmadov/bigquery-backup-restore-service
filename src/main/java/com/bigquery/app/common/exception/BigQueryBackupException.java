package com.bigquery.app.common.exception;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract sealed class BigQueryBackupException extends RuntimeException
        permits ResourceNotFoundException, PermissionException, ValidationException,
        ServiceException, ConflictException {

    private final HttpStatus status;
    private final String errorCode;

    protected BigQueryBackupException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}