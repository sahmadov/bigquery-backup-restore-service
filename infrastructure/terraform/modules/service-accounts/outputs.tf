output "cloud_run_sa_email" {
  description = "Email address of the Cloud Run service account"
  value       = google_service_account.cloud_run_sa.email
}

output "scheduler_sa_email" {
  description = "Email address of the Cloud Scheduler service account"
  value       = google_service_account.scheduler_sa.email
}