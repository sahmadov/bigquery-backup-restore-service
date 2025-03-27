# BigQuery Backup&Restore Automation

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
## Deployment Guide

### Option 1: Using the Deployment Script

The simplest way to deploy the service is using our deployment script:

```bash
./scripts/deploy/deploy-image.sh --project-id YOUR_GCP_PROJECT --registry YOUR_REGISTRY_NAME
```

For more details, see the [scripts/README.md](scripts/README.md) file.

### Option 2: Manual Docker Image Deployment

If you prefer to manually deploy the Docker image, follow these detailed steps:

#### Step 1: Google Cloud Authentication

```bash
# Login to your Google Cloud account
gcloud auth login

# Set your active project
gcloud config set project YOUR_PROJECT_ID

# Configure Docker to use Google Cloud credentials
gcloud auth configure-docker europe-west3-docker.pkg.dev
```

#### Step 2: Pull and Push the Image

Choose one of the following options depending on which registry you want to pull from:

**Option A: Pull from Google Artifact Registry**
```bash
# Pull the image from our GCP Artifact Registry
docker pull europe-west3-docker.pkg.dev/bigquery-automation-454819/bigquery-service-repo/bigquery-backup-restore-service:latest

# Tag the image for your own registry
docker tag europe-west3-docker.pkg.dev/bigquery-automation-454819/bigquery-service-repo/bigquery-backup-restore-service:latest \
    europe-west3-docker.pkg.dev/YOUR_PROJECT_ID/YOUR_REPOSITORY/bigquery-backup-restore-service:latest

# Push to your registry
docker push europe-west3-docker.pkg.dev/YOUR_PROJECT_ID/YOUR_REPOSITORY/bigquery-backup-restore-service:latest
```

**Option B: Pull from GitHub Packages**
```bash
# Create a GitHub Personal Access Token with read:packages scope
# Then login to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin

# Pull the image from GitHub Packages
docker pull ghcr.io/sahmadov/bigquery-backup-restore-service:latest

# Tag the image for your own registry
docker tag ghcr.io/sahmadov/bigquery-backup-restore-service:latest \
    europe-west3-docker.pkg.dev/YOUR_PROJECT_ID/YOUR_REPOSITORY/bigquery-backup-restore-service:latest

# Push to your registry
docker push europe-west3-docker.pkg.dev/YOUR_PROJECT_ID/YOUR_REPOSITORY/bigquery-backup-restore-service:latest
```

#### Step 3: Deploy to Cloud Run

1. Go to the Cloud Run service in your GCP Console
2. Click "Create Service"
3. Choose the option to "Deploy one revision from an existing container image"
4. Select the container image you pushed to your Artifact Registry
5. Configure the service settings:
    - Set memory to at least 1GiB
    - Set CPU to at least 1
    - Specify a service account with the following permissions:
        - BigQuery Data Editor
        - BigQuery User
        - Storage Admin roles

#### Step 4: Test the Service

1. Update the request files with your specific values:
   ```bash
   # Edit backup request file to match your environment
   nano scripts/examples/backup_request.json
   ```

2. Update the Cloud Run URL in the action script:
   ```bash
   # Edit the CLOUD_RUN_BASE_URL value to point to your deployed service
   nano scripts/action.sh
   ```

3. Run your first backup:
   ```bash
   ./scripts/action.sh backup ./scripts/examples/backup_request.json DEBUG
   ```

4. Run a restore operation:
   ```bash
   # Edit restore request file to use your backup details
   nano scripts/examples/restore_request.json
   
   # Run the restore
   ./scripts/action.sh restore ./scripts/examples/restore_request.json DEBUG
   ```

Remember to replace placeholder values:
- `YOUR_PROJECT_ID` with your GCP project ID
- `YOUR_REPOSITORY` with your Artifact Registry repository name
- `YOUR_GITHUB_USERNAME` with your GitHub username if using GitHub Packages

## Contributing

Contributions are welcome! Please check out our [contribution guidelines](CONTRIBUTING.md) first.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.