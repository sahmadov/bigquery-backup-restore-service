variable "service_name" {
  description = "The name of the Cloud Run Service"
  type        = string
}

variable "region" {
  description = "The region to deploy the Cloud Run Service"
  type        = string
}

variable "container_image" {
  description = "The container image to run in the Service"
  type        = string
}

variable "project_id" {
  description = "The GCP Project ID"
  type        = string
}

variable "cpu" {
  description = "CPU allocation for the Cloud Run Service"
  type        = string
  default     = "1"
}

variable "memory" {
  description = "Memory allocation for the Cloud Run Service"
  type        = string
  default     = "2Gi"
}

variable "service_account_email" {
  description = "The service account email to use for the Cloud Run service"
  type        = string
}