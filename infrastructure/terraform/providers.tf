terraform {
  required_version = ">= 1.0.0"

  required_providers {
    google = {
      source  = "hashicorp/google"
      version = ">= 4.0.0"
    }
  }

  backend "remote" {
    hostname     = "app.terraform.io"
    organization = "bigquery-backup-automation"

    workspaces {
      name = "bigquery-backup-restore-service"
    }
  }
}

provider "google" {
  credentials = var.google_credentials
  project     = local.project_id
  region      = local.region
}