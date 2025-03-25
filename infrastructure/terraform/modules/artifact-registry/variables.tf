variable "project_id" {
  description = "The GCP Project ID"
  type        = string
}

variable "repository_name" {
  description = "The name of the Artifact Registry repository"
  type        = string
}

variable "location" {
  description = "The location/region where the Artifact Registry repository will be created"
  type        = string
}
