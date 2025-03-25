package com.bigquery.app.common.util;

import com.google.cloud.bigquery.JobInfo;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
@NoArgsConstructor
public final class JobUtil {

    public static JobInfo.WriteDisposition resolveWriteDisposition(String writeDispositionString) {
        var disposition = switch (writeDispositionString.toUpperCase()) {
            case "WRITE_APPEND" -> JobInfo.WriteDisposition.WRITE_APPEND;
            case "WRITE_EMPTY" -> JobInfo.WriteDisposition.WRITE_EMPTY;
            default -> JobInfo.WriteDisposition.WRITE_TRUNCATE;
        };
        log.debug("Resolved write disposition '{}' to {}", writeDispositionString, disposition);
        return disposition;
    }

    public static Pattern createTableNamePattern(String format) {
        var patternString = "(?i)^([^-]+)-.*\\." + format;
        log.debug("Created table name pattern: {}", patternString);
        return Pattern.compile(patternString);
    }
}