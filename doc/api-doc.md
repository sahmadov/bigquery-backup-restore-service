# BigQuery Backup and Restore Service Documentation

## Table of Contents
- [API Endpoints](#api-endpoints)
- [Backup Request Format](#backup-request-format)
- [Restore Request Format](#restore-request-format)
- [Export Options](#export-options)
- [Import Options](#import-options)
- [Business Logic](#business-logic)
- [Limitations](#limitations)
- [Error Handling](#error-handling)
- [Thread Pool Configuration](#thread-pool-configuration)
- [Examples](#examples)

## API Endpoints

The service exposes two main REST endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/backup` | POST | Initiates a backup operation |
| `/api/v1/restore` | POST | Initiates a restore operation |

## Backup Request Format

### Top-level Structure

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `bigQuery` | Yes | Object | Contains BigQuery source details |
| `backupStorage` | Yes | Object | Contains backup storage configuration |
| `backupTime` | No | Object | Timestamp information for the backup (defaults to current time in UTC) |

### BigQuery Details (`bigQuery`)

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `projectId` | Yes | String | The source GCP project ID containing the BigQuery datasets |
| `tablesToBackup` | No | Array | List of datasets and tables to backup. If not provided, all datasets in the project will be discovered and backed up |

### Table Backup Specification (`tablesToBackup` array items)

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `datasetName` | Yes | String | The name of the dataset to backup |
| `tables` | No | Array | List of specific table names to backup. If not provided with `tableFilter`, all tables in the dataset will be backed up |
| `tableFilter` | No | String | Pattern to filter table names (supports wildcard `*`). For example, `"sales_*"` to back up all tables that start with "sales_" |

### Backup Storage (`backupStorage`)

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `gcsOptions` | Yes | Object | Google Cloud Storage configuration for backup |

### GCS Options (`gcsOptions`)

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `uri` | Yes | String | GCS URI (e.g., `"gs://my-backup-bucket"`) |
| `exportOptions` | Yes | Object | Configuration for the export process |

### Backup Time (`backupTime`)

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `localDateTime` | No | String | Local date and time in ISO format (default: current time) |
| `timezone` | No | String | Timezone ID (default: `"UTC"`) |

## Restore Request Format

### Top-level Structure

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `bigQuery` | Yes | Object | Contains BigQuery destination details |
| `restoreStorage` | Yes | Object | Contains restore storage configuration |

### BigQuery Restore Details (`bigQuery`)

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `projectId` | Yes | String | The destination GCP project ID where data will be restored |
| `datasetsToRestore` | Yes | Array | List of dataset names to restore |

### Restore Storage (`restoreStorage`)

| Field | Required | Type | Description |
|-------|----------|------|-------------|
| `gcsOptions` | Yes | Object | Google Cloud Storage configuration for restore |

### GCS Restore Options (`gcsOptions`)

| Field | Required | Type | Description                                                                                       |
|-------|----------|------|---------------------------------------------------------------------------------------------------|
| `uri` | Yes | String | GCS URI (e.g., `"gs://my-backup-bucket"`)                                                         |
| `backupTimestamp` | Yes | String | Timestamp of the backup to restore (format: `"YYYYMMDD_HHmmss"`)                                  |
| `projectIdOfBackup` | Yes | String | The source project ID where the backup was taken. It helps app to locate on correct folder in GCS |
| `importOptions` | Yes | Object | Configuration for the import process                                                              |

## Export Options

| Field | Required | Type | Default | Description |
|-------|----------|------|---------|-------------|
| `format` | Yes | String | "AVRO" | The export format (`"AVRO"`, `"CSV"`, `"JSON"`, `"PARQUET"`) |
| `overwrite` | Yes | Boolean | true | Whether to overwrite existing files in GCS |
| `header` | CSV Only | Boolean | true | Whether to include header row in CSV exports |
| `compression` | CSV Only | String | "GZIP" | Compression type for CSV exports |
| `fieldDelimiter` | CSV Only | String | "," | Field delimiter for CSV exports |
| `threadPoolSize` | No | Integer | 4 | Number of threads for parallel processing |
| `threadQueueCapacity` | No | Integer | 10 | Queue capacity for thread pool |

## Import Options

| Field | Required | Type | Default | Description |
|-------|----------|------|---------|-------------|
| `restoreRegion` | Yes | String | "europe-west3" | The GCP region for the restored dataset |
| `format` | Yes | String | "AVRO" | The import format (`"AVRO"`, `"CSV"`, `"JSON"`, `"PARQUET"`) |
| `header` | CSV Only | Boolean | true | Whether CSV files include a header row |
| `fieldDelimiter` | CSV Only | String | "," | Field delimiter for CSV files |
| `ignoreUnknownValues` | No | Boolean | true | Whether to ignore unknown fields in the import data |
| `maxBadRecords` | No | Integer | 0 | Maximum number of bad records allowed before failing |
| `writeDisposition` | No | String | "WRITE_TRUNCATE" | How to handle existing tables (`"WRITE_TRUNCATE"`, `"WRITE_APPEND"`, `"WRITE_EMPTY"`) |
| `threadPoolSize` | No | Integer | 5 | Number of threads for parallel processing |
| `threadQueueCapacity` | No | Integer | 15 | Queue capacity for thread pool |

## Business Logic

### GCP Permissions

Service account of your GCP container runner (in my case cloud run) should have this permissions/roles in target GCP project which is holding bigquery database:

   - `roles/bigquery.dataEditor` (on project level or dataset level) and `roles/bigquery.user`.
   - for gcs bucket we need `storage.buckets.get,storage.objects.list,storage.objects.create` if you have custom role you can use it, if not the only role which has all those permissions is `roles/storage.admin`

### Backup Process

1. **Dataset Discovery**: If no specific tables are provided, the service discovers all datasets in the project (excluding the snapshot dataset).
2. **Table Eligibility**:
    - Only standard tables are backed up; views, materialized views, and external tables are excluded.
    - Tables created after the backup time are excluded (when backup time is specified).
    - Tables matching the provided filter pattern or explicitly listed are included.
    - Only standard tables are backed up; views, materialized views, and external tables are excluded.
3. **Snapshot Process**:
    - For each eligible table, a snapshot is created in a dedicated snapshot dataset.
    - Snapshots have a default expiration of 1 day.
    - Snapshots are curicial components, as the all tables in datasets are snapshoted on current date time or user specified time.
4. **Export Process**:
    - Each snapshot table is exported to GCS in the specified format.
    - For CSV exports, additional parameters like headers, delimiters, and compression are applied.
    - Exports can be processed in parallel using the thread pool if `threadPoolSize` > 1.

### Restore Process

1. **Dataset Creation**:
    - For each dataset to be restored, a new dataset is created with the prefix "restored_".
    - The original dataset is not modified.
2. **Table Discovery**:
    - Tables are discovered from the backup files in GCS based on the provided backup timestamp.
    - Only files with the expected format extension are considered.
3. **Import Process**:
    - Each discovered table is imported into the appropriate destination dataset.
    - Import format and options are applied as specified.
    - Imports can be processed in parallel using the thread pool if `threadPoolSize` > 1.

## Limitations

1. **BigQuery Components**:
    - Dataset and table metadata (like access controls) is not preserved.
    - Routines, procedures, and UDFs are not backed up.

3. **Performance**:
    - Large tables may require optimized thread pool settings.
    - Very large datasets might hit GCP quotas or service limits.

## Error Handling

The service provides detailed error responses in case of failures:

| Error Type | HTTP Status | Description |
|------------|-------------|-------------|
| `RESOURCE_NOT_FOUND` | 404 | Requested resource (dataset, table, file) not found |
| `PERMISSION_DENIED` | 403 | Insufficient permissions to access resources |
| `RESOURCE_CONFLICT` | 409 | Resource already exists (e.g., snapshot table) |
| `VALIDATION_ERROR` | 400 | Invalid request parameters |
| `SERVICE_ERROR` | 500 | Internal service error |

## Thread Pool Configuration

The service supports configurable thread pools for optimizing performance:

### Backup Thread Pool

```properties
threadpool.backup.core-size=1
threadpool.backup.max-size=10
threadpool.backup.queue-capacity=25
threadpool.backup.name-prefix=backup-
```

### Restore Thread Pool

```properties
threadpool.restore.core-size=1
threadpool.restore.max-size=10
threadpool.restore.queue-capacity=25
threadpool.restore.name-prefix=restore-
```

## Examples

### Minimal Backup Request

```json
{
  "bigQuery": {
    "projectId": "my-gcp-project"
  },
  "backupStorage": {
    "gcsOptions": {
      "uri": "gs://my-backup-bucket/minimal-example",
      "exportOptions": {
        "format": "AVRO",
        "overwrite": true
      }
    }
  }
}
```

### Backup Request with Table Filtering

```json
{
  "bigQuery": {
    "projectId": "my-gcp-project",
    "tablesToBackup": [
      {
        "datasetName": "ecommerce_data",
        "tables": [
          "orders",
          "sales_2024_01"
        ]
      },
      {
        "datasetName": "analytics_dataset",
        "tableFilter": "monthly_*"
      }
    ]
  },
  "backupStorage": {
    "gcsOptions": {
      "uri": "gs://my-backup-bucket/csv-threaded",
      "exportOptions": {
        "format": "CSV",
        "overwrite": true,
        "header": true,
        "compression": "GZIP",
        "fieldDelimiter": ",",
        "threadPoolSize": 4,
        "threadQueueCapacity": 10
      }
    }
  },
  "backupTime": {
    "localDateTime": "2024-12-31T23:59:00",
    "timezone": "America/Los_Angeles"
  }
}
```

### Minimal Restore Request

```json
{
  "bigQuery": {
    "projectId": "my-restore-project",
    "datasetsToRestore": [
      "sales_data"
    ]
  },
  "restoreStorage": {
    "gcsOptions": {
      "uri": "gs://my-bucket",
      "backupTimestamp": "20240101_120000",
      "projectIdOfBackup": "source-project-123",
      "importOptions": {
        "restoreRegion": "europe-west3",
        "format": "AVRO",
        "writeDisposition": "WRITE_TRUNCATE"
      }
    }
  }
}
```

### Multi-threaded Restore Request

```json
{
  "bigQuery": {
    "projectId": "my-restore-project",
    "datasetsToRestore": [
      "user_logs",
      "purchase_history"
    ]
  },
  "restoreStorage": {
    "gcsOptions": {
      "uri": "gs://my-bucket",
      "backupTimestamp": "20240101_130000",
      "projectIdOfBackup": "source-project-456",
      "importOptions": {
        "restoreRegion": "europe-west3",
        "format": "CSV",
        "header": true,
        "fieldDelimiter": "|",
        "ignoreUnknownValues": false,
        "maxBadRecords": 5,
        "writeDisposition": "WRITE_TRUNCATE",
        "threadPoolSize": 5,
        "threadQueueCapacity": 50
      }
    }
  }
}
```