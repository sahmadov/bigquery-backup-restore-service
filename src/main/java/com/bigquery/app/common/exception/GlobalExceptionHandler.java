package com.bigquery.app.common.exception;

import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.storage.StorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.UUID;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BigQueryBackupException.class)
    public ResponseEntity<ErrorResponse> handleBigQueryBackupException(
            BigQueryBackupException ex, WebRequest request) {

        var errorId = generateErrorId();
        log.error("Error ID: {} - {}: {}", errorId, ex.getClass().getSimpleName(), ex.getMessage(), ex);

        var errorResponse = ErrorResponse.from(
                ex, ((ServletWebRequest) request).getRequest().getRequestURI());

        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {

        var errorId = generateErrorId();

        var errorDetails = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> String.format("'%s': %s", err.getField(), err.getDefaultMessage()))
                .collect(Collectors.joining(", "));

        log.error("Error ID: {} - Validation error: {}", errorId, errorDetails, ex);

        var validationException = new ValidationException("Validation failed: " + errorDetails);

        var errorResponse = ErrorResponse.from(
                validationException, ((ServletWebRequest) request).getRequest().getRequestURI());

        return ResponseEntity.status(validationException.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(BigQueryException.class)
    public ResponseEntity<ErrorResponse> handleBigQueryException(
            BigQueryException ex, WebRequest request) {

        var errorId = generateErrorId();
        log.error("Error ID: {} - BigQuery error: {} (Code: {})", errorId, ex.getMessage(), ex.getCode(), ex);

        BigQueryBackupException mappedException = switch (ex.getCode()) {
            case 404 -> new ResourceNotFoundException("BigQuery resource", ex.getMessage());
            case 403 -> new PermissionException("BigQuery resource", "access");
            case 409 -> new ConflictException("BigQuery resource", ex.getMessage());
            default -> new ServiceException("BigQuery", ex.getMessage(), ex);
        };

        var errorResponse = ErrorResponse.from(
                mappedException, ((ServletWebRequest) request).getRequest().getRequestURI());

        return ResponseEntity.status(mappedException.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponse> handleStorageException(
            StorageException ex, WebRequest request) {

        var errorId = generateErrorId();
        log.error("Error ID: {} - Storage error: {} (Code: {})", errorId, ex.getMessage(), ex.getCode(), ex);

        BigQueryBackupException mappedException = switch (ex.getCode()) {
            case 404 -> new ResourceNotFoundException("GCS resource", ex.getMessage());
            case 403 -> new PermissionException("GCS resource", "access");
            default -> new ServiceException("Google Cloud Storage", ex.getMessage(), ex);
        };

        var errorResponse = ErrorResponse.from(
                mappedException, ((ServletWebRequest) request).getRequest().getRequestURI());

        return ResponseEntity.status(mappedException.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(
            Exception ex, WebRequest request) {

        var errorId = generateErrorId();
        log.error("Error ID: {} - Unexpected error: {}", errorId, ex.getMessage(), ex);

        var wrappedException = new ServiceException(
                "Application",
                "An unexpected error occurred. Please try again later. (Error ID: " + errorId + ")"
        );

        var errorResponse = ErrorResponse.from(
                wrappedException, ((ServletWebRequest) request).getRequest().getRequestURI());

        return ResponseEntity.status(wrappedException.getStatus()).body(errorResponse);
    }

    private String generateErrorId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
