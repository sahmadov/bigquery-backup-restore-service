output "service_url" {
  description = "The URL of the deployed Cloud Run service."
  value       = google_cloud_run_service.backup_service.status[0].url
}