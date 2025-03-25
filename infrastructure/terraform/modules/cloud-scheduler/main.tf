resource "google_cloud_scheduler_job" "backup_scheduler" {
  name        = var.name
  description = var.description
  schedule    = var.schedule  # e.g. "0 2 * * *"
  time_zone   = "Europe/Berlin"

  http_target {
    http_method = "POST"

    uri = var.backend_uri

    body = base64encode(var.message_body)
    headers = {
      "Content-Type" = "application/json"
    }

    oidc_token {
      service_account_email = var.scheduler_sa_email
    }
  }
}