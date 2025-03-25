package com.bigquery.app.common.util;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;


@Slf4j
@NoArgsConstructor
public final class TimeUtil {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = ofPattern("yyyyMMdd_HHmmss");

    public static String formatTimestampForFileName(ZonedDateTime dateTime) {
        var formatted = dateTime.format(TIMESTAMP_FORMATTER);
        log.debug("Formatted timestamp: {} from datetime: {}", formatted, dateTime);
        return formatted;
    }
}