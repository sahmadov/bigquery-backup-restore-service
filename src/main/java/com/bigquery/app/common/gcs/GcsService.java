package com.bigquery.app.common.gcs;

import com.bigquery.app.common.exception.PermissionException;
import com.bigquery.app.common.exception.ResourceNotFoundException;
import com.bigquery.app.common.exception.ValidationException;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.bigquery.app.common.util.GcsPathUtil.extractBucketName;
import static com.bigquery.app.common.util.GcsPathUtil.getPathWithoutBucket;
import static java.util.Objects.isNull;

@Service
@Slf4j
public class GcsService {

    private final Storage storage;
    private final List<String> requiredPermissions;

    public GcsService(Storage storage,
                      @Value("${gcp.gcs.requiredPermissions}") String permissions) {
        this.storage = storage;
        this.requiredPermissions = Arrays.asList(permissions.split(","));
        log.info("GCS Service initialized with required permissions: {}", requiredPermissions);
    }

    public void validateGcsBucket(String gcsUri) {
        log.info("Validating GCS bucket for URI: {}", gcsUri);

        var bucketName = extractBucketName(gcsUri);
        validateBucketExists(bucketName);
        validateBucketPermissions(bucketName);

        log.info("GCS bucket validation successful for: {}", bucketName);
    }

    public Bucket getBucket(String gcsUri) {
        var bucketName = extractBucketName(gcsUri);
        var bucket = storage.get(bucketName);
        if (isNull(bucket)) {
            log.error("Bucket does not exist: {}", bucketName);
            throw new ResourceNotFoundException("GCS bucket", bucketName);
        }
        return bucket;
    }

    private void validateBucketExists(String bucketName) {
        var bucket = storage.get(bucketName);

        if (isNull(bucket)) {
            log.error("Bucket does not exist: {}", bucketName);
            throw new ResourceNotFoundException("GCS bucket", bucketName);
        }
    }

    private void validateBucketPermissions(String bucketName) {
        log.info("Checking IAM permissions for bucket: {}", bucketName);

        var grantedPermissions = storage.testIamPermissions(bucketName, requiredPermissions);

        if (grantedPermissions.size() != requiredPermissions.size() || grantedPermissions.contains(false)) {
            log.error("Insufficient permissions for bucket: {}. Required: {}, Granted: {}",
                    bucketName, requiredPermissions, grantedPermissions);
            throw new PermissionException("GCS bucket " + bucketName,
                    requiredPermissions.toString());
        }
    }

    public Set<String> findTableNamesFromGcsFiles(
            Bucket bucket,
            String basePath,
            Pattern pattern,
            String expectedFormat) {

        log.info("Discovering tables from GCS path: {}", basePath);
        var discoveredTables = new HashSet<String>();
        var pathWithoutBucket = getPathWithoutBucket(basePath);

        bucket.list(Storage.BlobListOption.prefix(pathWithoutBucket))
                .iterateAll()
                .forEach(blob -> {
                    var fileName = blob.getName().substring(blob.getName().lastIndexOf('/') + 1);
                    log.debug("Found file: {}", fileName);

                    Matcher matcher = pattern.matcher(fileName);
                    if (matcher.matches()) {
                        validateFileFormat(blob.getName(), expectedFormat);
                        var tableName = matcher.group(1);
                        log.info("Discovered table: {}", tableName);
                        discoveredTables.add(tableName);
                    }
                });

        return discoveredTables;
    }

    public void validateFileFormat(String fileName, String expectedFormat) {
        var actualExtension = fileName.substring(fileName.lastIndexOf('.') + 1);

        if (!actualExtension.equalsIgnoreCase(expectedFormat)) {
            log.error("File format mismatch. Expected: {}, Found: {} for file {}",
                    expectedFormat, actualExtension, fileName);
            throw new ValidationException(String.format("Extension mismatch. Expected: %s, Found: %s", expectedFormat, actualExtension));
        }
    }
}