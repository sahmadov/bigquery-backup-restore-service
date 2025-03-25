package com.bigquery.app.common.util;

import com.bigquery.app.common.config.GcsProperties;
import com.bigquery.app.common.exception.ValidationException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Optional.of;

@Slf4j
@NoArgsConstructor
public final class GcsPathUtil {
    private static final Pattern GCS_URI_PATTERN = Pattern.compile("^gs://([^/]+)(/.*)?$");
    private static String backupPath;

    public static void setBackupPath(GcsProperties gcsProperties) {
        backupPath = gcsProperties.getBackup().getPath();
    }

    public static String buildBackupFileUri(String baseUri,
                                            String timestamp,
                                            String projectId,
                                            String datasetName,
                                            String tableName,
                                            String format) {
        var uri = String.format("%s/%s/%s/%s/%s/%s-*.%s",
                baseUri, backupPath, timestamp, projectId, datasetName, tableName, format.toLowerCase());
        log.debug("Built backup file URI: {}", uri);
        return uri;
    }

    public static String buildBackupBasePath(String baseUri,
                                             String timestamp,
                                             String projectId,
                                             String datasetName) {
        var basePath = String.format("%s/%s/%s/%s/%s/",
                baseUri, backupPath, timestamp, projectId, datasetName);
        log.debug("Built backup base path: {}", basePath);
        return basePath;
    }

    public static String extractBucketName(String gcsUri) {
        return of(gcsUri)
                .map(GCS_URI_PATTERN::matcher)
                .filter(Matcher::matches)
                .map(matcher -> matcher.group(1))
                .orElseThrow(() -> {
                    log.error("Invalid GCS URI format: {}", gcsUri);
                    return new ValidationException("Invalid GCS URI format: " + gcsUri);
                });
    }

    public static String getPathWithoutBucket(String gcsPath) {
        var path = gcsPath.substring(5);
        var slashIndex = path.indexOf('/');
        return path.substring(slashIndex + 1);
    }
}