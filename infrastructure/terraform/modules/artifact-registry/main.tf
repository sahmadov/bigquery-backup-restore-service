resource "google_artifact_registry_repository" "artifact_registry" {
  repository_id = var.repository_name
  location      = var.location
  format        = "DOCKER"
}

resource "google_artifact_registry_repository_iam_member" "public_reader" {
  project    = var.project_id
  location   = var.location
  repository = var.repository_name
  role       = "roles/artifactregistry.reader"
  member     = "allUsers"
}