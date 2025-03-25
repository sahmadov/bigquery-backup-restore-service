resource "google_storage_bucket" "bucket" {
  name     = var.bucket_name
  location = var.location
  storage_class = var.storage_class
}

resource "google_storage_bucket_iam_member" "cloud_run_access" {
  bucket = google_storage_bucket.bucket.name
  role   = "roles/storage.admin"
  member = "serviceAccount:${var.cloud_run_sa_email}"
}