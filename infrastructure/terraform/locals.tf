locals {
  project_id       = "bigquery-automation-454819"
  region           = "europe-west3"
  backup_service_name     = "bigquery-backup-restore-service"
  backup_container_repo_name  = "bigquery-service-repo"
  backup_bucket_name      = "automated-bq-backup-restore-bucket"
  backup_bucket_storage_class= "ARCHIVE"

  backup_scheduler_cron = "0 2 * * *"  # Daily at 2 AM
  backup_scheduler_message= jsonencode({
    "bigQuery": {
      "projectId": "target-bigquery-host"
    },
    "backupStorage": {
      "gcsOptions": {
        "uri": "gs://automated-bq-backup-restore-bucket",
        "exportOptions": {
          "format": "AVRO",
          "overwrite": true
        }
      }
    }
  })

  restore_scheduler_cron = "0 6 * * *"  # Daily at 6 AM
  restore_scheduler_message= jsonencode({
    "bigQuery": {
      "projectId": "target-bigquery-host",
      "datasetsToRestore": [
        "ecommerce_data", "analytics_dataset"
      ]
    },
    "restoreStorage": {
      "gcsOptions": {
        "uri": "gs://automated-bq-backup-restore-bucket",
        "backupTimestamp": "20250325_074158",
        "projectIdOfBackup": "target-bigquery-host",
        "importOptions": {
          "format": "AVRO",
          "writeDisposition": "WRITE_TRUNCATE"
        }
      }
    }
  })
}
