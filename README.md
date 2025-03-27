# BigQuery Backup&Restore Automation

[![CI/CD Pipeline](https://github.com/sahmadov/bigquery-backup-restore-service/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/sahmadov/bigquery-backup-restore-service/actions/workflows/ci-cd.yml)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Code Coverage](https://img.shields.io/badge/coverage-75%25-green.svg)](https://github.com/sahmadov/bigquery-backup-restore-service)
[![Docker Image](https://img.shields.io/badge/Docker-ghcr.io-blue?logo=docker)](https://github.com/sahmadov/bigquery-backup-restore-service/pkgs/container/bigquery-backup-restore-service)

Open source project for backing up and restoring BigQuery datasets and tables.

## Features

- Automated backup of BigQuery datasets and tables
- Support for various export formats (Avro, Csv, Parquet, Json)
- Restore functionality with customizable options
- Multi-threaded operations for improved performance

## Architecture Overview

The BigQuery Backup and Restore solution provides automated backup and recovery capabilities for your BigQuery datasets and tables.

![BigQuery Backup and Restore Architecture](doc/bigquery-backup-and-restore-diagram.png)

### How It Works

Current architecture that I use consists of the following components:

1. **Cloud Scheduler** triggers backup and restore operations on a defined schedule.
   - Alternative triggering mechanisms can include Workflows, Pub/Sub, and Eventarc.

2. **Backup/Restore Service** (Cloud Run) orchestrates the entire process:
   - **Backup Flow (1.x):**
      - 1.1: Cloud Scheduler initiates the backup process
      - 1.2: Service processes tables for backup
      - 1.3: Tables are exported to Cloud Storage

   - **Restore Flow (2.x):**
      - 2.1: Cloud Scheduler initiates the restore process
      - 2.2: Service retrieves table information from Cloud Storage
      - 2.3: Tables are restored to BigQuery

3. **Cloud Storage** serves as the repository for all BigQuery backups.

4. **BigQuery** is the source and destination for data.

This containerized service can also be deployed on other compute platforms like GKE, GCE, or Cloud Functions depending on your requirements.

## How to Use

Before starting make sure you have created artifact registry for pushing images and cloud storage for saving backups.

#### Step 1: Google Cloud Authentication

```bash
# Login to your Google Cloud account
gcloud auth login

# Set your active project
gcloud config set project YOUR_PROJECT_ID

# Configure Docker to use Google Cloud credentials
gcloud auth configure-docker YOUR_GCP_REGION-docker.pkg.dev
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
If you want to test it as illustrated in above architecture diagram, you can follow this steps:
1. Go to the Cloud Run service in your GCP Console
2. Click "Create Service"
3. Choose the option to "Deploy one revision from an existing container image"
4. Select the container image you pushed to your Artifact Registry
5. Configure the service settings:
    - Set memory to at least 1GiB
    - Set CPU to at least 1
    - Specify a service account which has the following permissions:
        - BigQuery Data Editor
        - BigQuery User
        - Storage Admin roles

#### Step 4: Test the Service
request json files have actually way more options but for the sake of simplicity I use minimal valid json requests.
1. Update the request files with your specific values:
   ```bash
   # Edit backup request file with:
   # - bigQuery.projectId GCP project which is hosting bigquery database
   # - backupStorage.gcsOptions.uri which specifies where to save backups.
   nano scripts/examples/backup_request_minimal.json
   ```

2. Update the Cloud Run URL in the action script:
   ```bash
   # Edit the CLOUD_RUN_BASE_URL value to point to your deployed service
   nano scripts/action.sh
   ```

3. Run your first backup:
   ```bash
   ./scripts/action.sh backup ./scripts/examples/backup_request_minimal.json DEBUG
   ```

4. Run a restore operation:
   ```bash
   # Edit restore request with:
   # bigQuery.projectId - gcp project where you have your target bigquery running
   # bigQuery.datasetsToRestore - name of datasets that you want to restore
   # restoreStorage.gcsOptions.uri - cloud bucket where you saved your backups in previous backup step.
   # restoreStorage.gcsOptions.backupTimestamp - the date when backup is taken, please refer to gcs bucket in ordert to find exact date and format.
   # restoreStorage.gcsOptions.projectIdOfBackup - if Bigquery on which we took backup is different than were we want to restore, then this should be the original bigquery gcp project if not then this is usually same with bigQuery.projectId
   nano scripts/examples/restore_request_minimal.json.json
   
   # Run the restore
   ./scripts/action.sh restore ./scripts/examples/restore_request_minimal.json DEBUG
   ```

Remember to replace placeholder values:
- `YOUR_PROJECT_ID` with your GCP project ID
- `YOUR_REPOSITORY` with your Artifact Registry repository name
- `YOUR_GITHUB_USERNAME` with your GitHub username if using GitHub Packages
- `YOUR_GCP_REGION` with your preferred GCP region, such as europe-west3

## Contributing

Contributions are welcome! Please check out our [contribution guidelines](CONTRIBUTING.md) first.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.