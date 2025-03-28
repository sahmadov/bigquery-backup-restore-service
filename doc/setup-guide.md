# Project Setup Guide

## Overview

A step-by-step guide for setting up the BigQuery Backup & Restore Automation project infrastructure and deployment pipeline.

## Prerequisites

- Google Cloud Platform account with billing enabled
- GitHub account with repository admin access
- Terraform Enterprise account

## Setup Steps

### 1. Google Cloud Platform Setup

1. Create a new GCP project (e.g., `bigquery-automation-454819`)
2. Create a service account for Terraform with necessary roles:
    - Name: `terraform-enterprise-sa@bigquery-automation-454819.iam.gserviceaccount.com`
    - Generate and download a JSON key for this service account

### 2. Terraform Configuration

1. Create a Terraform Enterprise workspace named `bigquery-backup-restore-service`
2. Configure the workspace:
    - Working Directory: `/infrastructure/terraform`
    - Trigger Patterns: `/infrastructure/terraform/**/*`
    - Enable "Automatic speculative plans", this will send terraform plan to Pull requests which changes infra code.
3. Add the service account key as `var.google_credentials` in the Terraform workspace
4. For local development, store the key at a secure location and reference it in `terraform.local.auto.tfvars`. Current setting for me is something like this
   ```terraform
    google_credentials = "/path/to/your/key/location/key.json"
   ```
   
### 3. GitHub Repository Setup

1. Push code to the main branch of your repository (e.g., `https://github.com/username/bigquery-backup-restore-service`)
2. Create repository secrets:
    - `GCP_SA_KEY`: The contents of the Terraform service account key
    - `GH_PACKAGES_TOKEN`: GitHub Personal Access Token with 'write' permission for packages
3. Add repository variables for GitHub Actions:
    - `ARTIFACT_REGISTRY_LOCATION`: Your preferred GCP region (e.g., `europe-west3`)
    - `ARTIFACT_REPOSITORY`: Container repository name (e.g., `bigquery-service-repo`)
    - `GCP_PROJECT_ID`: Your GCP project ID (e.g., `bigquery-automation-454819`)

### 4. Project Configuration

1. Update `infrastructure/terraform/locals.tf` with your specific values:
    - `project_id`: Your GCP project ID
    - `region`: Your preferred GCP region
    - `backup_bucket_name`: Your GCS bucket name for backups, it has to be unique
2. Update all sample request JSON files in `scripts/examples/` to match your environment
3. Update scheduler message bodies to reference correct GCP projects and resources

### 5. Infrastructure Deployment

1. Run your first Terraform Enterprise deployment (note: Cloud Run and scheduler will fail initially)
2. Run the GitHub Actions CI/CD workflow on the main branch to build and deploy the application
3. Run Terraform Enterprise again to complete the Cloud Run and scheduler setup

### 6. BigQuery Test Data Setup

1. Connect to the target GCP project containing BigQuery
2. Set your default BigQuery location (e.g., `europe-west3` in query settings)
3. Run the `scripts/setup/bigquery_dummy_data.sql` script to create test datasets and tables

### 7. IAM Permissions Setup

1. Grant the Cloud Run service account the following permissions in target GCP project which has Bigquery Database:
    - `roles/bigquery.user` at the project level in target gcp project
    - `roles/bigquery.dataEditor` on relevant either datasets or project level in target gcp project
    - `roles/storage.admin` on gcp bucket 
   
   In my case I have provided all those access for `backup-restore-sa@bigquery-automation-454819.iam.gserviceaccount.com`

### 8. Testing the Deployment

1. Update `CLOUD_RUN_BASE_URL` in `scripts/action.sh` to your deployed Cloud Run service URL
2. Run backup operation: `./scripts/action.sh backup ./scripts/examples/backup_request.json DEBUG`
3. Run restore operation: `./scripts/action.sh restore ./scripts/examples/restore_request.json`
4. Test Cloud Scheduler triggers:
    - `trigger-bigquery-backup`
    - `trigger-restore-backup`
