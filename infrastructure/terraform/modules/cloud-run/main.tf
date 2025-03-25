resource "google_cloud_run_service" "backup_service" {
  name     = var.service_name
  location = var.region

  template {
    spec {
      service_account_name = var.service_account_email

      containers {
        image = var.container_image
        resources {
          limits = {
            cpu    = var.cpu
            memory = var.memory
          }
        }
      }
    }
  }
}