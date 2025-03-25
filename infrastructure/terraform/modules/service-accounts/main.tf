resource "google_service_account" "cloud_run_sa" {
  account_id   = "backup-restore-sa"
  display_name = "Cloud Run Service Account"
}

resource "google_service_account" "scheduler_sa" {
  account_id   = "cloud-scheduler-sa"
  display_name = "Cloud Scheduler Service Account"
}

resource "google_project_iam_member" "cloud_run_storage_access" {
  project = var.project_id
  role    = "roles/storage.objectAdmin"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

resource "google_project_iam_member" "cloud_run_artifact_registry_access" {
  project = var.project_id
  role    = "roles/artifactregistry.reader"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}

resource "google_project_iam_member" "scheduler_run_invoker" {
  project = var.project_id
  role    = "roles/run.invoker"
  member  = "serviceAccount:${google_service_account.scheduler_sa.email}"
}

resource "google_project_iam_member" "cloud_run_bigquery_user" {
  project = var.project_id
  role    = "roles/bigquery.user"
  member  = "serviceAccount:${google_service_account.cloud_run_sa.email}"
}