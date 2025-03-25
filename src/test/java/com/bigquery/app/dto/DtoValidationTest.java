package com.bigquery.app.dto;

import com.bigquery.app.backup.dto.ExportOptions;
import com.bigquery.app.backup.dto.TableToBackup;
import com.bigquery.app.restore.dto.ImportOptions;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static jakarta.validation.Validation.buildDefaultValidatorFactory;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.*;

class DtoValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Test ExportOptions validation for AVRO format")
    void testExportOptionsValidAvro() {
        // given
        ExportOptions options = new ExportOptions(
                "AVRO",
                true,
                null,
                null,
                null,
                null,
                null
        );

        // when
        Set<ConstraintViolation<ExportOptions>> violations = validator.validate(options);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Test ExportOptions validation for CSV format with required fields")
    void testExportOptionsValidCsv() {
        // given
        ExportOptions options = new ExportOptions(
                "CSV",
                true,
                true,
                "GZIP",
                ",",
                null,
                null
        );

        // when
        Set<ConstraintViolation<ExportOptions>> violations = validator.validate(options);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Test ExportOptions validation for CSV format missing required fields")
    void testExportOptionsInvalidCsvMissingFields() {
        // given
        ExportOptions options = new ExportOptions(
                "CSV",
                true,
                null,
                null,
                null,
                null,
                null
        );

        // when
        Set<ConstraintViolation<ExportOptions>> violations = validator.validate(options);

        // then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        ConstraintViolation<ExportOptions> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("CSV format"));
    }

    @Test
    @DisplayName("Test ExportOptions validation for invalid threadPoolSize")
    void testExportOptionsInvalidThreadPoolSize() {
        // given
        ExportOptions options = new ExportOptions(
                "AVRO",
                true,
                null,
                null,
                null,
                0,
                10
        );

        // when
        Set<ConstraintViolation<ExportOptions>> violations = validator.validate(options);

        // then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        ConstraintViolation<ExportOptions> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("threadPoolSize"));
    }

    @Test
    @DisplayName("Test ExportOptions validation for threadQueueCapacity without threadPoolSize")
    void testExportOptionsInvalidThreadQueueWithoutThreadPool() {
        // given
        ExportOptions options = new ExportOptions(
                "AVRO",
                true,
                null,
                null,
                null,
                null,
                10
        );

        // when
        Set<ConstraintViolation<ExportOptions>> violations = validator.validate(options);

        // then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        ConstraintViolation<ExportOptions> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("threadQueueCapacity"));
    }

    @Test
    @DisplayName("Test ImportOptions validation for AVRO format")
    void testImportOptionsValidAvro() {
        // given
        ImportOptions options = new ImportOptions(
                "europe-west3",
                "AVRO",
                TRUE,
                null,
                TRUE,
                0,
                "WRITE_TRUNCATE",
                null,
                null
        );

        // when
        Set<ConstraintViolation<ImportOptions>> violations = validator.validate(options);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Test ImportOptions validation for CSV format with required fields")
    void testImportOptionsValidCsv() {
        // given
        ImportOptions options = new ImportOptions(
                "europe-west3",
                "CSV",
                TRUE,
                ",",
                TRUE,
                0,
                "WRITE_TRUNCATE",
                null,
                null
        );

        // when
        Set<ConstraintViolation<ImportOptions>> violations = validator.validate(options);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Test ImportOptions validation for CSV format missing required fields")
    void testImportOptionsInvalidCsvMissingFields() {
        // given
        ImportOptions options = new ImportOptions(
                "europe-west3",
                "CSV",
                TRUE,
                null,
                TRUE,
                0,
                "WRITE_TRUNCATE",
                null,
                null
        );

        // when
        Set<ConstraintViolation<ImportOptions>> violations = validator.validate(options);

        // then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        ConstraintViolation<ImportOptions> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("CSV format"));
    }

    @Test
    @DisplayName("Test ImportOptions validation for invalid threadPoolSize")
    void testImportOptionsInvalidThreadPoolSize() {
        // given
        ImportOptions options = new ImportOptions(
                "europe-west3",
                "AVRO",
                TRUE,
                null,
                TRUE,
                0,
                "WRITE_TRUNCATE",
                0,
                10
        );
        // when
        Set<ConstraintViolation<ImportOptions>> violations = validator.validate(options);

        // then
        assertFalse(violations.isEmpty());
        assertEquals(1, violations.size());
        ConstraintViolation<ImportOptions> violation = violations.iterator().next();
        assertTrue(violation.getMessage().contains("threadPoolSize"));
    }

    @Test
    @DisplayName("Test TableToBackup validation with tables specified")
    void testTableToBackupValidWithTables() {
        // given
        Set<String> tables = new HashSet<>();
        tables.add("table1");

        TableToBackup tableToBackup = new TableToBackup(
                "test_dataset",
                tables,
                null
        );

        // when
        Set<ConstraintViolation<TableToBackup>> violations = validator.validate(tableToBackup);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Test TableToBackup validation with filter specified")
    void testTableToBackupValidWithFilter() {
        // given
        TableToBackup tableToBackup = new TableToBackup(
                "test_dataset",
                null,
                "table_*"
        );

        // when
        Set<ConstraintViolation<TableToBackup>> violations = validator.validate(tableToBackup);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Test TableToBackup validation with both tables and filter")
    void testTableToBackupValidWithTablesAndFilter() {
        // given
        Set<String> tables = new HashSet<>();
        tables.add("table1");

        TableToBackup tableToBackup = new TableToBackup(
                "test_dataset",
                tables,
                "table_*"
        );

        // when
        Set<ConstraintViolation<TableToBackup>> violations = validator.validate(tableToBackup);

        // then
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Test TableToBackup validation with missing datasetName")
    void testTableToBackupInvalidMissingDatasetName() {
        // given
        Set<String> tables = new HashSet<>();
        tables.add("table1");

        TableToBackup tableToBackup = new TableToBackup(
                null,
                tables,
                null
        );

        // when
        Set<ConstraintViolation<TableToBackup>> violations = validator.validate(tableToBackup);

        // then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("datasetName")));
    }
}