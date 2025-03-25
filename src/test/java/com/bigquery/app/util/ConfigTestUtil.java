package com.bigquery.app.util;

import com.bigquery.app.backup.dto.ExportOptions;
import com.bigquery.app.common.config.*;
import com.bigquery.app.common.util.GcsPathUtil;
import com.bigquery.app.restore.dto.ImportOptions;
import org.mockito.Mockito;

public class ConfigTestUtil {

    public static void setupConfigurationMocks() {
        setupBackupProperties();
        setupRestoreProperties();
        setupGcsProperties();
        setupExportProperties();
        setupImportProperties();
    }

    public static void setupBackupProperties() {
        BackupProperties.DatasetProperties.SnapshotProperties snapshotProps =
                Mockito.mock(BackupProperties.DatasetProperties.SnapshotProperties.class);
        Mockito.when(snapshotProps.getName()).thenReturn("table_snapshots_dataset");
        Mockito.when(snapshotProps.getPrefix()).thenReturn("_SNAPSHOT_");
        Mockito.when(snapshotProps.getExpirationDays()).thenReturn(1L);

        BackupProperties.DatasetProperties datasetProps = Mockito.mock(BackupProperties.DatasetProperties.class);
        Mockito.when(datasetProps.getSnapshot()).thenReturn(snapshotProps);

        BackupProperties backupProps = Mockito.mock(BackupProperties.class);
        Mockito.when(backupProps.getDataset()).thenReturn(datasetProps);
    }

    public static void setupRestoreProperties() {
        RestoreProperties.DatasetProperties datasetProps = Mockito.mock(RestoreProperties.DatasetProperties.class);
        Mockito.when(datasetProps.getPrefix()).thenReturn("restored_");

        RestoreProperties restoreProps = Mockito.mock(RestoreProperties.class);
        Mockito.when(restoreProps.getDataset()).thenReturn(datasetProps);
    }

    public static void setupGcsProperties() {
        GcsProperties.BackupProperties backupProps = Mockito.mock(GcsProperties.BackupProperties.class);
        Mockito.when(backupProps.getPath()).thenReturn("backups");

        GcsProperties gcsProps = Mockito.mock(GcsProperties.class);
        Mockito.when(gcsProps.getBackup()).thenReturn(backupProps);

        GcsPathUtil.setBackupPath(gcsProps);
    }

    public static void setupExportProperties() {
        ExportProperties.DefaultProperties defaultProps = Mockito.mock(ExportProperties.DefaultProperties.class);
        Mockito.when(defaultProps.getFormat()).thenReturn("AVRO");
        Mockito.when(defaultProps.isOverwrite()).thenReturn(true);
        Mockito.when(defaultProps.isHeader()).thenReturn(true);
        Mockito.when(defaultProps.getCompression()).thenReturn("GZIP");
        Mockito.when(defaultProps.getFieldDelimiter()).thenReturn(",");
        Mockito.when(defaultProps.getThreadPoolSize()).thenReturn(4);
        Mockito.when(defaultProps.getThreadQueueCapacity()).thenReturn(10);

        ExportProperties exportProps = Mockito.mock(ExportProperties.class);
        Mockito.when(exportProps.getDefaultProps()).thenReturn(defaultProps);

        ExportOptions.setDefaults(exportProps);
    }

    public static void setupImportProperties() {
        ImportProperties.DefaultProperties defaultProps = Mockito.mock(ImportProperties.DefaultProperties.class);
        Mockito.when(defaultProps.getFormat()).thenReturn("AVRO");
        Mockito.when(defaultProps.getHeader()).thenReturn(true);
        Mockito.when(defaultProps.getFieldDelimiter()).thenReturn(",");
        Mockito.when(defaultProps.getIgnoreUnknownValues()).thenReturn(true);
        Mockito.when(defaultProps.getMaxBadRecords()).thenReturn(0);
        Mockito.when(defaultProps.getWriteDisposition()).thenReturn("WRITE_TRUNCATE");
        Mockito.when(defaultProps.getRestoreRegion()).thenReturn("europe-west3");
        Mockito.when(defaultProps.getThreadPoolSize()).thenReturn(5);
        Mockito.when(defaultProps.getThreadQueueCapacity()).thenReturn(15);

        ImportProperties importProps = Mockito.mock(ImportProperties.class);
        Mockito.when(importProps.getDefaultProps()).thenReturn(defaultProps);

        ImportOptions.setDefaults(importProps);
    }
}