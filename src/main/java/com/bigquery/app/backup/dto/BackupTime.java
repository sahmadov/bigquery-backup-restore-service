package com.bigquery.app.backup.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BackupTime(
        @NotNull(message = "datetime must not be empty if backupTime is provided")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime localDateTime,

        @NotBlank(message = "timezone must not be empty if backupTime is provided")
        String timezone
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");

    @JsonCreator
    public BackupTime {
        localDateTime = nonNull(localDateTime) ? localDateTime : LocalDateTime.now();
        timezone = nonNull(timezone) ? timezone : "UTC";
    }

    public ZonedDateTime toUtcZonedDateTime() {
        return "UTC".equalsIgnoreCase(timezone)
                ? localDateTime.atZone(ZoneId.of("UTC"))
                : localDateTime.atZone(ZoneId.of(timezone)).withZoneSameInstant(ZoneId.of("UTC"));
    }

    public String toFormattedUtcString() {
        return toUtcZonedDateTime().format(FORMATTER);
    }
}