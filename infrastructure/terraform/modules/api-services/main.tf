resource "google_project_service" "gcp_resource_manager_api" {
  project = var.project_id
  service = "cloudresourcemanager.googleapis.com"
}

resource "google_project_service" "artifact_registry" {
  project = var.project_id
  service = "artifactregistry.googleapis.com"
  depends_on = [google_project_service.gcp_resource_manager_api]
}

resource "google_project_service" "iam" {
  project = var.project_id
  service = "iam.googleapis.com"
  depends_on = [google_project_service.gcp_resource_manager_api]
}

resource "google_project_service" "cloud_run" {
  project = var.project_id
  service = "run.googleapis.com"
  depends_on = [google_project_service.gcp_resource_manager_api]
}

resource "google_project_service" "container_registry" {
  project = var.project_id
  service = "containerregistry.googleapis.com"
  depends_on = [google_project_service.gcp_resource_manager_api]
}

resource "google_project_service" "cloud_scheduler" {
  project = var.project_id
  service = "cloudscheduler.googleapis.com"
  depends_on = [google_project_service.gcp_resource_manager_api]
}

resource "google_project_service" "storage" {
  project                    = var.project_id
  service                    = "storage.googleapis.com"
  disable_on_destroy         = true
  disable_dependent_services = true
  depends_on = [google_project_service.gcp_resource_manager_api]
}
