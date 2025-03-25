package com.bigquery.app.restore.dto;

import com.bigquery.app.common.config.ImportProperties;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.util.StringUtils;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImportOptions(
        @NotBlank String restoreRegion,
        @NotBlank String format,
        Boolean header,
        String fieldDelimiter,
        Boolean ignoreUnknownValues,
        Integer maxBadRecords,
        @NotBlank String writeDisposition,
        Integer threadPoolSize,
        Integer threadQueueCapacity
) {
    private static ImportProperties.DefaultProperties defaults;

    @JsonCreator
    public ImportOptions(
            @JsonProperty("restoreRegion") String restoreRegion,
            @JsonProperty("format") String format,
            @JsonProperty("header") Boolean header,
            @JsonProperty("fieldDelimiter") String fieldDelimiter,
            @JsonProperty("ignoreUnknownValues") Boolean ignoreUnknownValues,
            @JsonProperty("maxBadRecords") Integer maxBadRecords,
            @JsonProperty("writeDisposition") String writeDisposition,
            @JsonProperty("threadPoolSize") Integer threadPoolSize,
            @JsonProperty("threadQueueCapacity") Integer threadQueueCapacity
    ) {
        this.restoreRegion = nonNull(restoreRegion) ? restoreRegion : defaults.getRestoreRegion();
        this.format = nonNull(format) ? format : defaults.getFormat();
        this.header = nonNull(header) ? header : defaults.getHeader();
        this.fieldDelimiter = fieldDelimiter;
        this.ignoreUnknownValues = nonNull(ignoreUnknownValues) ? ignoreUnknownValues : defaults.getIgnoreUnknownValues();
        this.maxBadRecords = nonNull(maxBadRecords) ? maxBadRecords : defaults.getMaxBadRecords();
        this.writeDisposition = nonNull(writeDisposition) ? writeDisposition : defaults.getWriteDisposition();
        this.threadPoolSize = threadPoolSize;
        this.threadQueueCapacity = threadQueueCapacity;
    }

    public static void setDefaults(ImportProperties importProperties) {
        defaults = importProperties.getDefaultProps();
    }

    @AssertTrue(message = "For CSV format, 'fieldDelimiter' and 'header' must be provided.")
    public boolean isCsvOptionsValid() {
        if ("CSV".equalsIgnoreCase(format)) {
            return StringUtils.hasText(fieldDelimiter) && Boolean.TRUE.equals(header);
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
