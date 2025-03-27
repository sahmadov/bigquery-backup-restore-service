# BigQuery Backup Automation

[![CI/CD Pipeline](https://github.com/sahmadov/bigquery-backup-restore-service/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/sahmadov/bigquery-backup-restore-service/actions/workflows/ci-cd.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Code Coverage](https://img.shields.io/badge/coverage-75%25-green.svg)](https://github.com/sahmadov/bigquery-backup-restore-service)
[![Docker Image](https://img.shields.io/badge/Docker-ghcr.io-blue?logo=docker)](https://github.com/sahmadov/bigquery-backup-restore-service/pkgs/container/bigquery-backup-restore-service)

Open source project for backing up and restoring BigQuery datasets and tables.

## Features

- Automated backup of BigQuery datasets and tables
- Configurable backup schedule via Cloud Scheduler
- Support for various export formats (AVRO, CSV)
- Restore functionality with customizable options
- Multi-threaded operations for improved performance
- Cloud-native deployment with GCP Cloud Run

## Architecture

This service runs as a containerized application on Cloud Run and provides REST APIs for backup and restore operations. It uses Google Cloud Storage for storing the backups.

## Getting Started

### Prerequisites

- Google Cloud Platform account
- BigQuery datasets to backup
- Google Cloud Storage bucket for storing backups
- Terraform for infrastructure deployment

### Deployment

The project can be deployed using Terraform:

```bash
cd infrastructure/terraform
terraform init
terraform plan
terraform apply
```

### Configuration

The application uses Spring Boot configuration properties which can be customized in `application.properties`.

## API Reference

### Backup API

```
POST /api/v1/backup
```

Example request:

```json
{
  "bigQuery": {
    "projectId": "my-gcp-project",
    "tablesToBackup": [
      {
        "datasetName": "my_dataset",
        "tables": ["table1", "table2"],
        "tableFilter": "prefix_*"
      }
    ]
  },
  "backupStorage": {
    "gcsOptions": {
      "uri": "gs://my-backup-bucket",
      "exportOptions": {
        "format": "AVRO",
        "overwrite": true
      }
    }
  }
}
```

### Restore API

```
POST /api/v1/restore
```

Example request:

```json
{
  "bigQuery": {
    "projectId": "my-gcp-project",
    "datasetsToRestore": ["my_dataset"]
  },
  "restoreStorage": {
    "gcsOptions": {
      "uri": "gs://my-backup-bucket",
      "backupTimestamp": "20250325_070424",
      "projectIdOfBackup": "source-project-id",
      "importOptions": {
        "format": "AVRO",
        "writeDisposition": "WRITE_TRUNCATE"
      }
    }
  }
}
```
## Deployment Options

### Option 1: Using the Deployment Script

The simplest way to deploy the service is using our deployment script:

```bash
./scripts/deploy/deploy-image.sh --project-id YOUR_GCP_PROJECT --registry YOUR_REGISTRY_NAME
```

For more details, see the [scripts/README.md](scripts/README.md) file.

### Option 2: Manual Docker Image Deployment

If you prefer to manually deploy the Docker image, you can use these commands:

1. Authenticate with Google Cloud:
```bash
gcloud auth login
gcloud config set project YOUR_PROJECT_ID
```

2. Configure Docker to use gcloud credentials for your Artifact Registry:
```bash
gcloud auth configure-docker REGION-docker.pkg.dev
```

3. Pull the image from one of our public registries:
```bash
# From GitHub Packages
docker pull ghcr.io/sahmadov/bigquery-backup-restore-service:latest

# OR from Google Artifact Registry
docker pull europe-west3-docker.pkg.dev/bigquery-automation-454819/bigquery-service-repo/bigquery-backup-restore-service:latest
```

4. Tag the image for your Artifact Registry:
```bash
docker tag ghcr.io/sahmadov/bigquery-backup-restore-service:latest \
    REGION-docker.pkg.dev/YOUR_PROJECT_ID/YOUR_REPOSITORY/bigquery-backup-restore-service:latest
```

5. Push the image to your Artifact Registry:
```bash
docker push REGION-docker.pkg.dev/YOUR_PROJECT_ID/YOUR_REPOSITORY/bigquery-backup-restore-service:latest
```

Remember to replace:
- `REGION` with your GCP region (e.g., europe-west3)
- `YOUR_PROJECT_ID` with your GCP project ID
- `YOUR_REPOSITORY` with your Artifact Registry repository name

## Contributing

Contributions are welcome! Please check out our [contribution guidelines](CONTRIBUTING.md) first.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.