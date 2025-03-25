package com.bigquery.app.backup.dto;

import com.bigquery.app.common.config.ExportProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

import static java.util.Objects.*;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExportOptions(
        @NotBlank String format,
        @NotNull Boolean overwrite,
        Boolean header,
        String compression,
        String fieldDelimiter,
        Integer threadPoolSize,
        Integer threadQueueCapacity
) {
    private static ExportProperties.DefaultProperties defaults;

    @JsonCreator
    public ExportOptions(
            @JsonProperty("format") String format,
            @JsonProperty("overwrite") Boolean overwrite,
            @JsonProperty("header") Boolean header,
            @JsonProperty("compression") String compression,
            @JsonProperty("fieldDelimiter") String fieldDelimiter,
            @JsonProperty("threadPoolSize") Integer threadPoolSize,
            @JsonProperty("threadQueueCapacity") Integer threadQueueCapacity
    ) {

        this.format = nonNull(format) ? format : defaults.getFormat();
        this.overwrite = nonNull(overwrite) ? overwrite : defaults.isOverwrite();
        this.header = header;
        this.compression = compression;
        this.fieldDelimiter = fieldDelimiter;
        this.threadPoolSize = threadPoolSize;
        this.threadQueueCapacity = threadQueueCapacity;
    }

    public static void setDefaults(ExportProperties exportProperties) {
        defaults = exportProperties.getDefaultProps();
    }

    @AssertTrue(message = "For CSV format, 'header', 'compression', and 'fieldDelimiter' must be provided.")
    public boolean isCsvOptionsValid() {
        if ("CSV".equalsIgnoreCase(format)) {
            return nonNull(header) && hasText(compression) && hasText(fieldDelimiter);
        }
        return true;
    }

    @AssertTrue(message = "If threadPoolSize is specified, it must be greater than 0")
    public boolean isThreadPoolSizeValid() {
        return isNull(threadPoolSize) || threadPoolSize > 0;
    }

    @AssertTrue(message = "If threadQueueCapacity is specified, threadPoolSize must also be specified")
    public boolean isThreadQueueCapacityValid() {
        return isNull(threadQueueCapacity) || nonNull(threadPoolSize);
    }

    public boolean isMultiThreaded() {
        return nonNull(threadPoolSize) && threadPoolSize > 1;
    }
}
